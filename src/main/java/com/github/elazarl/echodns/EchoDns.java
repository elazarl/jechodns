package com.github.elazarl.echodns;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
        return new NameService() {
            String suffix = System.getProperty("echodns.suffix", "");
            Pattern cannonicalIpRe = Pattern.compile("a(\\d+)-(\\d+)-(\\d+)-(\\d+)" +
                    "(" + Pattern.quote(suffix) + ")?");
            @Override
            public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
                Matcher matcher = cannonicalIpRe.matcher(s);
                if (matcher.matches()) {
                    return new InetAddress[] {
                            InetAddress.getByName(
                                    matcher.group(1) + "." +
                                    matcher.group(2) + "." +
                                    matcher.group(3) + "." +
                                    matcher.group(4)
                            )
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
