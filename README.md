# EchoDns resolver

## Goal

Hadoop service stack, requires every node to have an available DNS service.
Every IP in hadoop cluster must be reverse-resolvable to a hostname, and this
hostname must resolve back to the original IP by each node in the cluster.

Usually, one would run a central DNS server for his hadoop cluster.
For non-production use, I wanted to be more flexible, and I didn't want to depend
on DNS server.

If you use Sun java (either OpenJDK or the
binary java from Oracle), you can specify a class to be used as name resolver
for the JVM. `EchoDns` makes it easy to specify itself as the DNS
resolver for the JVM.

## Usage

EchoDns will convert each IPv4 to a canonical hostname and vice versa.
For example, 1.1.1.1 will be converted to a1-1-1-1, 2.3.4.5 to a2-3-4-5 etc.

If you need FQDN, it's possible to add suffix to the resolved name.
EchoDns will append the content of the property `echodns.suffix` to the resolved name.
Given `-Dechodns.suffix`, both a1-1-1-1 and a1-1-1-1.example.com will be
resolved to 1.1.1.1.

Typical usage:
    java -cp echodns.jar -Dsun.net.spi.nameservice.provider.1=dns,echodns -jar foo.jar
    java -cp echodns.jar -Dsun.net.spi.nameservice.provider.1=dns,echodns \
        -Dechodns.suffix -jar foo.jar

Note that injecting nameservice provider through `sun.net.spi.nameservice.provider` API
is supported only in Sun's java, and is
[not guaranteed to stay](http://docs.oracle.com/javase/7/docs/technotes/guides/net/properties.html#jndi).
