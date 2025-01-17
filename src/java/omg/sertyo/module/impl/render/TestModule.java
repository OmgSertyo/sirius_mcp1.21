package omg.sertyo.module.impl.render;


import com.darkmagician6.eventapi.EventTarget;
import omg.sertyo.Babulka;
import omg.sertyo.event.render.EventRender2D;
import omg.sertyo.manager.dragging.DragManager;
import omg.sertyo.manager.dragging.Draggable;
import omg.sertyo.module.Category;
import omg.sertyo.module.Module;
import omg.sertyo.module.ModuleAnnotation;
import omg.sertyo.module.setting.impl.ColorSetting;
import omg.sertyo.module.setting.impl.ModeSetting;
import omg.sertyo.utility.render.RenderUtility;

import java.awt.Color;

@ModuleAnnotation(
        name = "TestModule",
        category = Category.RENDER
)
public class TestModule extends Module {
    private final Draggable testdrag = DragManager.create(this, "testdrag", 10, 300);
    public TestModule() {

        this.testdrag.setWidth(50);
        this.testdrag.setHeight(50);
        this.testdrag.setY(4);
        this.testdrag.setX(10);
    }
    @EventTarget
    public void onRender(EventRender2D event) {
        RenderUtility.drawRoundedRect(this.testdrag.getX(), this.testdrag.getY(), this.testdrag.getWidth(), this.testdrag.getHeight(), 4, Babulka.getInstance().getThemeManager().getCurrentStyleTheme().getColors()[0].getRGB());
    }
}
