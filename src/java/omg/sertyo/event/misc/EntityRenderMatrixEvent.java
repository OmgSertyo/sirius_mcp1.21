package omg.sertyo.event.misc;

import com.darkmagician6.eventapi.events.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.Entity;

@Getter
@AllArgsConstructor
public final class EntityRenderMatrixEvent implements Event {
    private final PoseStack matrix;
    private final Entity entity;
}
