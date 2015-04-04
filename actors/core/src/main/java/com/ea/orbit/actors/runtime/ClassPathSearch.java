package com.ea.orbit.actors.runtime;

import com.ea.orbit.concurrent.ConcurrentHashSet;
import com.ea.orbit.util.ClassPath;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Internal class to locate interface implementations.
 */
public class ClassPathSearch
{
    private final ConcurrentHashSet<String> unprocessed = new ConcurrentHashSet<>();

    private final ConcurrentHashMap<Class<?>, Class<?>> concreteImplementations = new ConcurrentHashMap<>();
    private Class<?>[] classesOfInterest;

    public ClassPathSearch(Class<?>... classesOfInterest)
    {
        this.classesOfInterest = classesOfInterest;
        // get all class names from class path
        ClassPath.get().getAllResources().stream()
                .map(r -> r.getResourceName())
                .filter(rn -> rn.endsWith(".class"))
                .map(rn -> rn.substring(0, rn.length() - 6).replace('/', '.'))
                .forEach(cn -> unprocessed.add(cn));

    }

    public <T, R extends T> Class<R> findImplementation(Class<T> theInterface)
    {
        Class<?> implementationClass = concreteImplementations.get(theInterface);
        if (implementationClass != null)
        {
            return (Class<R>) implementationClass;
        }
        if (unprocessed.size() == 0)
        {
            return null;
        }

        final String expectedName = theInterface.getName();
        // cloning the list of unprocessed since it might be modified by the operations on the stream.

        // searching
        implementationClass = Stream.of(unprocessed.toArray())
                .map(o -> (String) o)
                .sorted((a, b) -> {
                    // order by closest name to the interface, closest beginning then closest ending.
                    int sa = commonStart(expectedName, a);
                    int sb = commonStart(expectedName, b);
                    return (sa != sb) ? (sb - sa) : (commonEnd(expectedName, b) - commonEnd(expectedName, a));
                })
                .map(cn -> {
                    // this returns non null if there is a match
                    // it also culls the list
                    try
                    {
                        Class clazz = Class.forName(cn);
                        if (!clazz.isInterface() && theInterface.isAssignableFrom(clazz))
                        {
                            // when searching for IHello1, must avoid:
                            // IHello1 <- IHello2
                            // IHello1 <- HelloImpl1
                            // IHello2 <- HelloImpl2  (wrong return, lest strict.

                            // However the application **should not** do this kind of class tree.

                            // Important: this makes ClassPathSearch non generic.
                            for (Class<?> i : clazz.getInterfaces())
                            {
                                if (i != theInterface && theInterface.isAssignableFrom(i))
                                {
                                    return null;
                                }
                            }
                            // found the best match!
                            return clazz;
                        }
                        for (Class<?> base : classesOfInterest)
                        {
                            if (base.isAssignableFrom(clazz))
                            {
                                // keep the classes that are part of the classes of interest list
                                return null;
                            }
                        }
                        // culling the list for the next search
                        unprocessed.remove(cn);
                        return null;
                    }
                    catch (Throwable e)
                    {
                        // there is some problem with this class
                        // culling the list for the next search
                        unprocessed.remove(cn);
                        return null;
                    }
                })
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);

        if (implementationClass != null)
        {
            // cache the findings.
            concreteImplementations.put(theInterface, implementationClass);
        }
        return (Class<R>) implementationClass;
    }


    /**
     * Returns the size of the common start the two strings
     * <p/>
     * Example: {@code commonStart("ssssBBB", "ssCCC") == 2 }
     */
    static int commonStart(String a, String b)
    {
        int i = 0, l = Math.min(a.length(), b.length());
        for (; i < l && a.charAt(i) == b.charAt(i); i++)
        {
        }
        return i;
    }

    /**
     * Returns the size of the common end the two strings
     * <p/>
     * Example: {@code commonEnd("AAAAeeee", "BBBee") == 2 }
     */
    static int commonEnd(String a, String b)
    {
        int c = 0;
        int ia = a.length();
        int ib = b.length();

        for (; --ia >= 0 && --ib >= 0 && a.charAt(ia) == b.charAt(ib); )
        {
            c++;
        }
        return c;
    }

}
