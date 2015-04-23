package com.ea.orbit.actors.runtime;

import org.junit.Test;

import static com.ea.orbit.actors.runtime.ClassPathSearch.commonEnd;
import static com.ea.orbit.actors.runtime.ClassPathSearch.commonStart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ClassPathSearchTest
{
    @Test
    public void testUtilityFunctions()
    {
        assertEquals(0, commonStart("BBB", "CCC"));
        assertEquals(0, commonEnd("AAAA", "BBB"));

        assertEquals(0, commonStart("BBB", ""));
        assertEquals(0, commonEnd("AAAA", ""));
        assertEquals(0, commonStart("", "A"));
        assertEquals(0, commonEnd("", "A"));

        assertEquals(1, commonStart("A", "A"));

        assertEquals(1, commonStart("ssssBBB", "sCCC"));
        assertEquals(2, commonStart("ssssBBB", "ssCCC"));
        assertEquals(3, commonStart("ssssBBB", "sssCCC"));

        assertEquals(1, commonStart("sBBB", "sssCCC"));
        assertEquals(2, commonStart("ssBBB", "sssCCC"));
        assertEquals(3, commonStart("sssBBB", "sssCCC"));

        assertEquals(1, commonEnd("AAAAe", "BBBee"));
        assertEquals(2, commonEnd("AAAAeeee", "BBBee"));
        assertEquals(3, commonEnd("AAAAeeee", "BBBeee"));
    }

    public interface IHi
    {
    }

    public class Hi implements IHi
    {
    }

    @Test
    public void testFind()
    {
        assertSame(Hi.class, new ClassPathSearch().findImplementation(IHi.class));
    }

    public interface IPla1
    {
    }

    public interface IPla2 extends IPla1
    {
    }

    public interface IPla3
    {
    }

    public interface IPla4
    {
    }

    public class Pla1 implements IPla1
    {
    }

    public class Pla2 implements IPla2
    {
    }

    public class XPla4 implements IPla3
    {
    }

    public class XPla3 implements IPla4
    {
    }

    @Test
    public void testFindWithSubInterfaces()
    {
        ClassPathSearch classPathSearch = new ClassPathSearch();
        assertSame(Pla1.class, classPathSearch.findImplementation(IPla1.class));
        assertSame(Pla2.class, classPathSearch.findImplementation(IPla2.class));
    }

    @Test
    public void testEarlyRemovalBug()
    {
        ClassPathSearch classPathSearch = new ClassPathSearch();
        assertSame(XPla3.class, classPathSearch.findImplementation(IPla4.class));
        assertSame(XPla4.class, classPathSearch.findImplementation(IPla3.class));
    }

    public interface IMul
    {
    }

    public abstract class Mul implements IMul
    {
    }

    public class MulImpl extends Mul
    {
    }

    @Test
    public void testFindWithAbstractClass()
    {
        ClassPathSearch classPathSearch = new ClassPathSearch();
        assertSame(MulImpl.class, classPathSearch.findImplementation(IMul.class));
    }

    public interface IFail
    {
    }

    @Test
    public void testFailedSearch()
    {
        ClassPathSearch classPathSearch = new ClassPathSearch();
        // without limit this would do a full classpath search...
        assertNull(classPathSearch.findImplementation(IFail.class, 200));
    }
}
