package fr.aquazus.d1proxy.handlers;

import fr.aquazus.d1proxy.Proxy;
import fr.aquazus.d1proxy.network.ProxyClient;
import simplenet.packet.Packet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AXKHandler implements PacketHandler {

    private Proxy proxy;
    private List<Character> zkArray = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_');

    public AXKHandler(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean shouldForward(ProxyClient proxyClient, String packet) {
        System.out.println("[" + proxyClient.getIp() + "] Intercepted AXK. Forging and flushing a new one!");
        proxy.getExchangeCache().put(proxyClient.getIp(), decodeAXK(packet));
        String newPacket = "AXK" + encodeAXK(proxy.getConfiguration().getProxyIp() + ":" + proxy.getConfiguration().getProxyPort()) + packet.substring(14);
        Packet.builder().putBytes(newPacket.getBytes()).putByte(0).writeAndFlush(proxyClient.getClient());
        return false;
    }

    private String decodeAXK(String rawPacket) {
        String obfIpPort = rawPacket.substring(3);
        String obfIp = obfIpPort.substring(0, 8);
        String obfPort = obfIpPort.substring(8, 11);
        int ip = 0;
        for (int i = 0; i < 8; i++) {
            int pos = 4 * (7 - i);
            ip |= (((obfIp.charAt(i) - 48) & 15) << pos);
        }
        int port = 0;
        for (int i = 0; i < 3; i++) {
            int pos = 6 * (2 - i);
            port |= (decode64(obfPort.charAt(i)) & 63) << pos;
        }
        return int2ip(ip) + ":" + port;
    }

    private String encodeAXK(String ipPort) {
        int indx = ipPort.indexOf(':');
        int ip = ip2int(ipPort.substring(0, indx));
        int port = Integer.parseInt(ipPort.substring(indx + 1));

        char[] obfIp = new char[8];
        for (int i = 0; i < 8; i++) {
            int pos = 4 * i;
            obfIp[i % 2 == 0 ? i + 1 : i - 1] = (char) (((ip >> pos) & 15) + 48);
        }

        char[] obfPort = new char[3];
        for (int i = 0; i < 3; i++) {
            int pos = 6 * (2 - i);
            obfPort[i] = encode64((port >> pos) & 63);
        }

        return new String(obfIp) + new String(obfPort);
    }

    private int decode64(char data) {
        return zkArray.indexOf(data);
    }

    private char encode64(int data) {
        return zkArray.get(data);
    }

    private String int2ip(int ip) {
        return IntStream.of(
                ip >> 24 & 0xff,
                ip >> 16 & 0xff,
                ip >> 8 & 0xff,
                ip & 0xff)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining("."));
    }

    private int ip2int(String ip) {
        String[] parts = ip.split("\\.");
        int iip = 0;
        for (int i = 0; i < parts.length; i++) {
            iip |= Integer.parseInt(parts[i]) << (i * 8);
        }
        return iip;
    }
}
