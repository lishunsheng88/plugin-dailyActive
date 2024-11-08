package run.halo.dailyActive;

import org.apache.tika.utils.DateUtils;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import static run.halo.app.extension.index.IndexAttributeFactory.multiValueAttribute;
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author guqing
 * @since 1.0.0
 */
@Component
public class StarterPlugin extends BasePlugin {

    private SchemeManager schemeManager;

    public StarterPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        System.out.println("插件启动成功！");
        schemeManager.register(InterfaceLogInfo.class, indexSpecs -> {
                indexSpecs.add(new IndexSpec()
                    .setName("spec.username")
                    .setIndexFunc(simpleAttribute(InterfaceLogInfo.class, interfaceLogInfo -> {
                        String username = interfaceLogInfo.getSpec().getUsername();
                        return username == null ? "" : username;
                    }))
                );
                indexSpecs.add(new IndexSpec()
                    .setName("spec.clientIp")
                    .setIndexFunc(simpleAttribute(InterfaceLogInfo.class, interfaceLogInfo -> {
                        String clientIp = interfaceLogInfo.getSpec().getClientIp();
                        return clientIp == null ? "" : clientIp;
                    }))
                );
                indexSpecs.add(new IndexSpec()
                    .setName("spec.path")
                    .setIndexFunc(simpleAttribute(InterfaceLogInfo.class, interfaceLogInfo -> {
                        String path = interfaceLogInfo.getSpec().getPath();
                        return path == null ? "" : path;
                    }))
                );
                indexSpecs.add(new IndexSpec()
                    .setName("spec.accessTime")
                    .setIndexFunc(simpleAttribute(InterfaceLogInfo.class, interfaceLogInfo -> {
                        Date accessTime = interfaceLogInfo.getSpec().getAccessTime();
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                        return accessTime == null ? null : formatter.format(accessTime);
                    }))
                );
            }
        );
    }

    @Override
    public void stop() {
        System.out.println("插件停止！");
    }
}
