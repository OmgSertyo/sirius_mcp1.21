package omg.sertyo.event.player;

import com.darkmagician6.eventapi.events.Event;
import net.minecraft.world.entity.Entity;

public class EventDamageEntityItem implements Event {
   private Entity entity;

   public Entity getEntity() {
      return this.entity;
   }

   public EventDamageEntityItem(Entity entity) {
      this.entity = entity;
   }
}
