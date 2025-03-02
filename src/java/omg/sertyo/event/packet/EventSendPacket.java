package omg.sertyo.event.packet;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.network.protocol.Packet;

public class EventSendPacket extends EventCancellable {
   private final Packet<?> packet;

   public EventSendPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
