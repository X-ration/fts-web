package com.adam.ftsweb.config;

import com.adam.ftsweb.controller.UserController;
import com.adam.ftsweb.controller.WebSocketController;
import com.adam.ftsweb.service.FriendRelationshipService;
import com.adam.ftsweb.service.MessageService;
import com.adam.ftsweb.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
@EnableWebMvc
@EnableWebSocket
public class WebConfig implements WebMvcConfigurer, ApplicationListener<WebServerInitializedEvent> {

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static int SERVER_PORT = 0;

    public static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/" };

    @Autowired
    private LoginInterceptor loginInterceptor;
    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    /**
     * 允许在请求路径中包含特殊字符
     * @return
     */
    @Bean
    public TomcatServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers((Connector connector) -> {
            connector.setProperty("relaxedPathChars", "\"<>[\\]^`{|}");
            connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}");
        });
        return factory;
    }

    /**
     * 获取端口号
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        SERVER_PORT = event.getWebServer().getPort();
    }

    /**
     * 解决No mapping for GET /bootstrap/css/bootstrap.min.css.map
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);

    }

    /**
     * 登录拦截器配置
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //避免拦截静态资源，手动添加需要拦截的路径
        registry.addInterceptor(loginInterceptor).addPathPatterns("/", "/index", "/ws", "/file/upload");
    }

    /**
     * 解决Jackson日期格式因为配置拦截器继承自WebMvcConfigurer失效的问题
     * @param converters the list of configured converters to be extended
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = builder.createXmlMapper(false)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER))
                .serializerByType(LocalTime.class, new LocalTimeSerializer(TIME_FORMATTER))
                .deserializerByType(LocalTime.class, new LocalDateTimeDeserializer(TIME_FORMATTER))
                .serializerByType(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER))
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER))
                .build();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);
    }

    @Bean
    public ViewResolver viewResolver(@Autowired SpringTemplateEngine templateEngine) {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        return viewResolver;
    }

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    /**
     * Thymeleaf layout dialect
     */
    @Bean
    public SpringTemplateEngine templateEngine(@Autowired SpringResourceTemplateResolver templateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        exporter.setAnnotatedEndpointClasses(WebSocketController.class);
        return exporter;
    }

    @Autowired
    public void setWebSocketUserService(UserService userService, FriendRelationshipService friendRelationshipService,
                                        MessageService messageService, UserController userController, ObjectMapper objectMapper) {
        WebSocketController.setUserService(userService, friendRelationshipService, messageService, userController, objectMapper);
    }

}
