package com.ea.orbit.bridge;

import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.OrbitContainer;
import com.ea.orbit.container.Startable;
import com.ea.orbit.util.ClassPath;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class BridgeServer implements Startable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BridgeServer.class);

    @Config("orbit.bridge.rest.port")
    private int port = 8182;

    @Inject
    private OrbitContainer container;
    private Set<Class> actorInterfaces;
    private List<BridgeInfo> bridgeInfos;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Task<Void> start() {

        try {

            OrbitStage stage = container.get(OrbitStage.class);


            Component component = new Component();
            Context context = new Context();
            component.getServers().add(Protocol.HTTP, port);
            Application restapp = new Application(context);
            Router router = new Router(context);
            restapp.setInboundRoot(router);



            //TODO find a better way to find actor interfaces, for annotation lookup
            //currently is limited limited to finding those starting with com.ea.orbit

            actorInterfaces = new HashSet<>();
            bridgeInfos = new ArrayList<>();
            context.getAttributes().put("bridgeInfos", bridgeInfos);

            final List<ClassPath.ResourceInfo> allClasses = ClassPath.get().getAllResources();
            List<Class> availableClasses = new ArrayList<>();
            for (ClassPath.ResourceInfo info : allClasses) {
                if (info.getResourceName().endsWith(".class")) {
                    String name = info.getResourceName().substring(0, info.getResourceName().length() - 6).replace("/", ".");
                    if (name.contains("com.ea.orbit")) {
                        try {
                            Class c = Class.forName(name);
                            availableClasses.add(c);
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                }
            }

            for (Class c : availableClasses) {
                if (c.isInterface()) {
                    for (Method m : c.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(Bridge.class)) {
                            actorInterfaces.add(c);
                        }
                    }
                }
            }

            for (Class c : availableClasses) {
                if (!c.isInterface())
                    for(Class i : actorInterfaces) {
                        if (i.isAssignableFrom(c)&&!c.isMemberClass()) {

                            for (Method m : i.getDeclaredMethods()) {
                                if (m.isAnnotationPresent(Bridge.class)) {


                                    String path = m.getDeclaredAnnotation(Bridge.class).path();
                                    System.out.println("found bridge in " + c.getSimpleName() + "." + m.getName()+" "+path);
                                    router.attach(path, BridgeResource.class);

                                    BridgeInfo bridgeInfo = new BridgeInfo();
                                    bridgeInfo.interfaceClass = i;
                                    bridgeInfo.actorClass = c;
                                    bridgeInfo.method= m;
                                    bridgeInfo.path = path;
                                    bridgeInfos.add(bridgeInfo);
                                }
                            }

                        }
                    }
            }

            component.getDefaultHost().attachDefault(restapp);
            component.start();

        }catch(Exception e){
            e.printStackTrace();
        }

        return Task.done();
    }

    public class BridgeInfo {
        public Class interfaceClass;
        public Class actorClass;
        public String path;
        public Method method;
    }

    public Task<Void> stop(){
        return Task.done();
    }


}
