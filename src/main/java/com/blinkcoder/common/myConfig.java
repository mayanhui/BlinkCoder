package com.blinkcoder.common;


import com.blinkcoder.controller.*;
import com.blinkcoder.model.*;
import com.blinkcoder.plugin.visitStat.VisitStatPlugin;
import com.blinkcoder.render.VelocityToolboxRenderFactory;
import com.jfinal.config.*;
import com.jfinal.kit.PathKit;
import com.jfinal.kit.StringKit;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.ehcache.EhCachePlugin;
import com.jfinal.render.ViewType;
import com.qiniu.api.config.Config;

import java.io.File;
import java.util.Properties;

/**
 * User: Michael
 * Date: 13-10-10
 * Time: 下午9:23
 */
public class myConfig extends JFinalConfig {

    private String json = java.lang.System.getenv("VCAP_SERVICES");
    private boolean isLocal = StringKit.isBlank(json);
    private Properties conf = null;

    @Override
    public void configConstant(Constants me) {
        conf = loadPropertyFile("classes" + File.separator + "config.txt");
        myConstants.VELOCITY_TEMPLETE_PATH = getProperty("velocity_templete_path");
        if (isLocal) {
            me.setDevMode(true);
        }
        me.setViewType(ViewType.OTHER);
        me.setMainRenderFactory(new VelocityToolboxRenderFactory());

        me.setError404View(myConstants.VELOCITY_TEMPLETE_PATH + "/404.html");
        me.setError500View(myConstants.VELOCITY_TEMPLETE_PATH + "/500.html");

    }


    @Override
    public void configRoute(Routes me) {
        me.add("/blog", BlogController.class).add("/catalog", CatalogController.class)
                .add("/label", LabelController.class).add("/link", LinkController.class)
                .add("/user", UserController.class).add("/qiniu", QiNiuController.class);
    }

    @Override
    public void configPlugin(Plugins me) {
        String jdbcUrl, username, password, driver;
        driver = getProperty("driverClass");
        jdbcUrl = getProperty("jdbcUrl");
        username = getProperty("username");
        password = getProperty("password");
        DruidPlugin druidPlugin = new DruidPlugin(jdbcUrl, username, password, driver);
        druidPlugin.setInitialSize(3).setMaxActive(10);
        me.add(druidPlugin);


        // 配置ActiveRecord插件
        ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);

        // 缓存插件
        me.add(new EhCachePlugin(PathKit.getWebRootPath() + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "ehcache.xml"));
//        me.add(new RedisPlugin("113.116.200.231", 6379, 0));
//        arp.setCache(new Redis());

        me.add(new VisitStatPlugin());

        if (isLocal) {
            arp.setShowSql(true);
        }
        arp.addMapping("blog", Blog.class).addMapping("user", User.class).addMapping("catalog", Catalog.class)
                .addMapping("blog_label", BlogLabel.class).addMapping("label", Label.class).addMapping("link", Link.class);
        me.add(arp);
    }

    @Override
    public void configInterceptor(Interceptors me) {

    }

    @Override
    public void configHandler(Handlers me) {
    }

    @Override
    public void afterJFinalStart() {
        myConstants.STATIC_RESOURCE_PATH = getProperty("static_resource_path");
        myConstants.QINIU_BUICKET = getProperty("qiniu_buicket");
        Config.ACCESS_KEY = getProperty("qiniu_access_key");
        Config.SECRET_KEY = getProperty("qiniu_secret_key");
    }

    @Override
    public void beforeJFinalStop() {
        VisitStatPlugin.daemon.stop();
    }
}
