package spacegraph.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import jcog.io.Serials;
import jcog.net.UDPeer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

/** TODO in progress */
public abstract class MeterPeer {
    public final UDPeer udp;

    protected MeterPeer() throws IOException {
        this(new UDPeer());
    }

    protected MeterPeer(UDPeer udp) {
        this.udp = udp;
        udp.fps(10);
    }

    private static class Subscription {
        final InetSocketAddress from, to;
        final String[] on;

        private Subscription(InetSocketAddress from, InetSocketAddress to, String[] on) {
            this.from = from;
            this.to = to;
            this.on = on;
        }
    }

    static final byte SCAN = (byte) 200;
    static final byte INDEX = (byte) 201;

    public static class MeterSupplier extends MeterPeer {

        protected MeterSupplier() throws IOException {
            super();
            udp.receive.on(m -> {
                switch (m.cmd()) {
                    case SCAN:
                        try {
                            udp.send(INDEX, index(), m.from);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            });
        }

        public Map<String,Object> index() {
            return Map.of("x", "string", "y", "float");
        }

    }

    public static class MeterConsumer extends MeterPeer {

        protected MeterConsumer() throws IOException {
            super();
            udp.receive.on(m -> {
                switch (m.cmd()) {
                    case INDEX:
                        try {
                            index(m.from.addr, Serials.fromBytes(m.data(), Map.class));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            });

            scan();
        }

        public void index(InetSocketAddress from, Map<String,Object> index) {

        }

        public void scan() {
            try {
                udp.tellSome(SCAN, Boolean.TRUE, 2);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new MeterSupplier() {

        };

        Util.sleepMS(1000);

        new MeterConsumer() {

            @Override
            public void index(InetSocketAddress from, Map<String, Object> index) {
                super.index(from, index);
                System.out.println("INDEX: " + from + " " + index);
                //TODO subscribe
            }
        };

        Util.sleepMS(1000);
    }
}