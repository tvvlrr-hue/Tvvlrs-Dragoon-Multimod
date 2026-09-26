package legend.multimod;

import legend.core.renderer.Obj;
import legend.core.renderer.Translucency;

import javax.annotation.Nullable;

public class FastTravelDummyObj extends Obj {
  public FastTravelDummyObj() {
    super("FastTravel Dummy");
  }

  @Override
  protected void performDelete() {
  }

  @Override
  public boolean hasTexture() {
    return false;
  }

  @Override
  public boolean hasTexture(final int index) {
    return false;
  }

  @Override
  public boolean hasTranslucency() {
    return false;
  }

  @Override
  public boolean hasTranslucency(final int index) {
    return false;
  }

  @Override
  public boolean shouldRender(@Nullable final Translucency translucency) {
    return false;
  }

  @Override
  public boolean shouldRender(@Nullable final Translucency translucency, final int layer) {
    return false;
  }

  @Override
  public int getLayers() {
    return 0;
  }

  @Override
  public void render(final int layer, final int startVertex, final int vertexCount) {
  }

  @Override
  public void render(@Nullable final Translucency translucency, final int layer, final int startVertex, final int vertexCount) {
  }
}
