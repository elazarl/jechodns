package com.github.elazarl.echodns;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * {@code EchoDns} will convert each IPv4 to a canonical hostname and vice versa.
 * For example, 1.1.1.1 will be converted to a1-1-1-1, 2.3.4.5 to a2-3-4-5 etc.
 * </p>
 * <p>
 * It's possible to add suffix to the resolved name, to make them FQDN, by
 * using the property {@code echodns.suffix}, so that both a1-1-1-1 and
 * a1-1-1-1.example.com will be resolved to 1.1.1.1.
 * </p>
 *
 * <p>
 * The intended use case is to inject {@code EchoDns} as the main DNS service
 * to the JVM, thus, allowing to run hadoop clusters on machines with no DNS
 * service.
 * </p>
 *
 * Typical usage:
 *
 * <pre>
 * java -cp echodns.jar -Dsun.net.spi.nameservice.provider.1=dns,echodns -jar foo.jar
 * java -cp echodns.jar -Dsun.net.spi.nameservice.provider.1=dns,echodns -Dechodns.suffix -jar foo.jar
 * </pre>
 *
 */
public class EchoDns implements NameServiceDescriptor {
    @Override
    public NameService createNameService() throws Exception {
        ServiceLoader<NameServiceDescriptor> nsds = ServiceLoader.load(NameServiceDescriptor.class);
        final List<NameService> otherNs = new ArrayList<NameService>();
        for (NameServiceDescriptor nsd : nsds) {
            if (nsd.getClass() == EchoDns.class) continue;
            otherNs.add(nsd.createNameService());
        }
        return new NameService() {
            String hostname;
            Object impl;
            Method lookupAllHostAddrMeth;

            InetAddress[] nativeLookupAllHostAddr(String s) throws UnknownHostException {
                try {
                    return (InetAddress[]) lookupAllHostAddrMeth.invoke(impl, s);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException e) {
                    if (e.getTargetException().getClass().equals(UnknownHostException.class)) {
                        throw (UnknownHostException)e.getTargetException();
                    }
                    throw new RuntimeException(e.getTargetException());
                }
            }

            {
                Field f = InetAddress.class.getDeclaredField("impl");
                f.setAccessible(true);
                impl = f.get(null);
                Method m = impl.getClass().getDeclaredMethod("getLocalHostName", new Class[]{});
                m.setAccessible(true);
                hostname = (String) m.invoke(impl);
                lookupAllHostAddrMeth = impl.getClass().getDeclaredMethod("lookupAllHostAddr", new Class[]{String.class});
                lookupAllHostAddrMeth.setAccessible(true);
            }

            String suffix = System.getProperty("echodns.suffix", "");
            Pattern cannonicalIpRe = Pattern.compile("a(\\d+)-(\\d+)-(\\d+)-(\\d+)" +
                    "(" + Pattern.quote(suffix) + ")?");
            @Override
            public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
                if (hostname.equals(s) || hostname.equals("localhost")) {
                    try {
                        final InetAddress[] nativeAddr = nativeLookupAllHostAddr(s);
                        final InetAddress[] resolved = new InetAddress[nativeAddr.length];
                        for (int i = 0; i < nativeAddr.length; i++) {
                            final byte[] address = nativeAddr[i].getAddress();
                            resolved[i] = InetAddress.getByAddress(
                                    getHostByAddr(address),
                                    address);
                        }
                        return resolved;
                    } catch (UnknownHostException uhe) {
                        // if native resolution didn't work - try others
                    }
                    UnknownHostException uhe = null;
                    for (NameService ns : otherNs) {
                        try {
                            return ns.lookupAllHostAddr(s);
                        } catch (UnknownHostException e) {
                            uhe = e;
                        }
                    }
                    throw uhe;
                }
                Matcher matcher = cannonicalIpRe.matcher(s);
                if (matcher.matches()) {
                    final InetAddress address = InetAddress.getByName(
                            matcher.group(1) + "." +
                                    matcher.group(2) + "." +
                                    matcher.group(3) + "." +
                                    matcher.group(4)
                    );
                    return new InetAddress[] {
                            InetAddress.getByAddress(getHostByAddr(address.getAddress()),
                                    address.getAddress())
                    };
                }
                throw new UnknownHostException("unknown host " + s);
            }

            @Override
            public String getHostByAddr(byte[] bytes) throws UnknownHostException {
                InetAddress address = InetAddress.getByAddress(bytes);
                if (address instanceof Inet4Address) {
                    Inet4Address ipv4 = (Inet4Address) address;
                    return "a" + ipv4.getHostAddress().replace('.', '-') + suffix;
                }
                return null;
            }
        };
    }

    @Override
    public String getProviderName() {
        return "echodns";
    }

    @Override
    public String getType() {
        return "dns";
    }
}
