package omg.sertyo.event.packet;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.network.protocol.Packet;

public class EventPacket extends EventCancellable implements Event {

    private Packet packet;

    public EventPacket(Packet packet) {
        this.packet = packet;
    }

    public Packet getPacket() {
        return packet;
    }



}
