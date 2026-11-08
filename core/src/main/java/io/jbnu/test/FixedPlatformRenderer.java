package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.BufferUtils;

/** Render-only grass/dirt patches at a uniform scale, cropped to the original solid rectangle. */
public final class FixedPlatformRenderer implements Disposable {
    public static final float SCALE = .25f;
    public static final float GRASS_HEIGHT = 24f;
    public static final boolean OUTLINE_ENABLED = true;
    public static final int OUTLINE_COLOR_RGBA = 0x514D38FF;
    public static final int OUTLINE_PIXELS = 1;
    public static final float OUTLINE_ALPHA_THRESHOLD = .10f;
    private static final long MAX_OUTLINE_BYTES = 32L * 1024 * 1024;
    private final Texture[] textures = new Texture[4];
    private final Pixmap[] sources = new Pixmap[4];
    private final Patch soil, grass, longGrass, left, right;
    private final LongMap<CachedOutline> outlines = new LongMap<>();
    private float pixelsPerWorldUnit = 1;
    private boolean outlineEnabled = OUTLINE_ENABLED;
    private int maxTextureSize, buildCount;
    private long cachedBytes, buildNanos;

    private static final class CachedOutline {
        Texture texture;
        float scale;
        long bytes;
    }

    private static final class Patch {
        final Texture texture;
        final Pixmap source;
        final float minimumAlpha;
        final int x, y, width, height;
        Patch(Texture texture, Pixmap source, int x, int y, int width, int height) {
            if (x+width > texture.getWidth() || y+height > texture.getHeight())
                throw new GdxRuntimeException("Grass image too small for material patch");
            this.texture = texture; this.x = x; this.y = y; this.width = width; this.height = height;
            this.source = source;
            int minimum=255;
            for(int sy=y;sy<y+height;sy++)for(int sx=x;sx<x+width;sx++)minimum=Math.min(minimum,source.getPixel(sx,sy)&255);
            minimumAlpha=minimum/255f;
        }
    }

    public FixedPlatformRenderer() {
        String[] names = {"grass_left.png","grass_center.png","grass_right.png","grass_long_reference.png"};
        String loading = "";
        try {
            for (int i = 0; i < names.length; i++) {
                loading = "pixel/platforms/"+names[i];
                sources[i] = new Pixmap(Gdx.files.internal(loading));
                textures[i] = new Texture(sources[i]);
                textures[i].setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            }
            // Interior samples avoid transparent scalloped bottoms and differently sized outer silhouettes.
            soil = new Patch(textures[1],sources[1],40,156,224,64);
            grass = new Patch(textures[1],sources[1],16,20,280,96);
            longGrass = new Patch(textures[3],sources[3],300,70,400,96);
            left = new Patch(textures[0],sources[0],160,70,64,96);
            right = new Patch(textures[2],sources[2],16,60,64,96);
            java.nio.IntBuffer limit=BufferUtils.newIntBuffer(1);
            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE,limit);maxTextureSize=limit.get(0);
        } catch (RuntimeException error) {
            dispose();
            throw new GdxRuntimeException("Failed to initialize fixed platform art: "+loading,error);
        }
    }

    public void render(SpriteBatch batch, Rectangle bounds) {
        if(outlineEnabled&&bounds.width>0&&bounds.height>0)renderOutline(batch,bounds);
        float top = bounds.y+bounds.height;
        float grassHeight = Math.min(GRASS_HEIGHT,bounds.height);
        if (bounds.height > grassHeight) fill(batch,soil,bounds.x,bounds.y,bounds.width,bounds.height-grassHeight);
        // The visual walking surface is exactly y + height. All layers stay within the collision rectangle.
        fill(batch,bounds.width >= 480 ? longGrass : grass,bounds.x,top-grassHeight,bounds.width,grassHeight);
        float edgeWidth = Math.min(16,bounds.width/2);
        fill(batch,left,bounds.x,top-grassHeight,edgeWidth,grassHeight);
        fill(batch,right,bounds.x+bounds.width-edgeWidth,top-grassHeight,edgeWidth,grassHeight);
    }

    /** Call before SpriteBatch.begin; translation does not invalidate the cache. */
    public void setPixelsPerWorldUnit(float scale) {
        if(!Float.isFinite(scale)||scale<=0)throw new IllegalArgumentException("Positive platform screen scale required");
        float stable=Math.max(1f/1024,Math.round(scale*1024)/1024f);
        if(stable!=pixelsPerWorldUnit){clearOutlines();pixelsPerWorldUnit=stable;}
    }
    public void setOutlineEnabled(boolean enabled) { outlineEnabled=enabled; }
    public boolean isOutlineEnabled() { return outlineEnabled; }

    private void renderOutline(SpriteBatch batch,Rectangle b) {
        long key=((long)Float.floatToIntBits(b.width)<<32)|(Float.floatToIntBits(b.height)&0xffffffffL);
        CachedOutline entry=outlines.get(key);
        if(entry==null){
            long started=System.nanoTime();
            entry=buildOutline(b.width,b.height);
            buildNanos+=System.nanoTime()-started;buildCount++;
            if(cachedBytes+entry.bytes>MAX_OUTLINE_BYTES){batch.flush();clearOutlines();}
            outlines.put(key,entry);cachedBytes+=entry.bytes;
        }
        float color=batch.getPackedColor();boolean blend=batch.isBlendingEnabled();
        batch.setColor(1,1,1,1);if(!blend)batch.enableBlending();
        batch.draw(entry.texture,b.x-OUTLINE_PIXELS/entry.scale,b.y-OUTLINE_PIXELS/entry.scale,
            entry.texture.getWidth()/entry.scale,entry.texture.getHeight()/entry.scale);
        batch.setPackedColor(color);if(!blend)batch.disableBlending();
    }

    private CachedOutline buildOutline(float width,float height) {
        int pad=OUTLINE_PIXELS;
        float scale=Math.min(pixelsPerWorldUnit,Math.min((maxTextureSize-2f*pad)/width,(maxTextureSize-2f*pad)/height));
        scale=Math.min(scale,(float)Math.sqrt((MAX_OUTLINE_BYTES/4.0)/(width*height))*.99f);
        int w=(int)Math.ceil(width*scale)+2*pad,h=(int)Math.ceil(height*scale)+2*pad;
        boolean[] visible=new boolean[w*h];
        for(int y=pad;y<h-pad;y++)for(int x=pad;x<w-pad;x++){
            float wx=(x-pad+.5f)/scale,wy=(y-pad+.5f)/scale;
            if(wx<width&&wy<height)visible[y*w+x]=alphaAt(wx,wy,width,height)>=OUTLINE_ALPHA_THRESHOLD;
        }
        boolean[] band=PlatformOutlineMask.create(visible,w,h,OUTLINE_PIXELS);
        Pixmap pixmap=new Pixmap(w,h,Pixmap.Format.RGBA8888);
        try{
            // Convert bottom-up mask coordinates to the PNG/Texture top-down rows.
            pixmap.setColor(0,0,0,0);pixmap.fill();
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(band[y*w+x])pixmap.drawPixel(x,h-1-y,OUTLINE_COLOR_RGBA);
            CachedOutline entry=new CachedOutline();entry.texture=new Texture(pixmap);
            entry.texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            entry.scale=scale;entry.bytes=(long)w*h*4;return entry;
        }finally{pixmap.dispose();}
    }

    private float alphaAt(float x,float y,float width,float height) {
        float grassHeight=Math.min(GRASS_HEIGHT,height),edgeWidth=Math.min(16,width/2),a=0;
        if(y<height-grassHeight)a=sample(soil,x,y,width,height-grassHeight);
        else a=sample(width>=480?longGrass:grass,x,y-height+grassHeight,width,grassHeight);
        if(y>=height-grassHeight){
            if(x<edgeWidth){float s=sample(left,x,y-height+grassHeight,edgeWidth,grassHeight);a=s+a*(1-s);}
            if(x>=width-edgeWidth){float s=sample(right,x-width+edgeWidth,y-height+grassHeight,edgeWidth,grassHeight);a=s+a*(1-s);}
        }
        return a;
    }
    private float sample(Patch p,float x,float y,float width,float height) {
        // If every source texel is visible, its exact alpha cannot change the mask.
        if(p.minimumAlpha>=OUTLINE_ALPHA_THRESHOLD)return p.minimumAlpha;
        float tileW=p.width*SCALE,tileH=p.height*SCALE;
        float startX=(float)Math.floor(x/tileW)*tileW,down=height-y;
        float startY=(float)Math.floor(down/tileH)*tileH;
        float drawW=Math.min(tileW,width-startX),drawH=Math.min(tileH,height-startY);
        int sw=Math.round(drawW/SCALE),sh=Math.round(drawH/SCALE);
        int sx=p.x+Math.min(sw-1,(int)((x-startX)/drawW*sw));
        int sy=p.y+Math.min(sh-1,(int)((down-startY)/drawH*sh));
        return (p.source.getPixel(sx,sy)&255)/255f;
    }

    private void clearOutlines() {
        for(CachedOutline entry:outlines.values())entry.texture.dispose();
        outlines.clear();cachedBytes=0;
    }
    int getOutlineBuildCount(){return buildCount;}
    long getOutlineBuildNanos(){return buildNanos;}
    long getCachedOutlineBytes(){return cachedBytes;}
    int getCachedOutlineCount(){return outlines.size;}

    private void fill(SpriteBatch batch, Patch patch, float x, float y, float width, float height) {
        float tileWidth = patch.width*SCALE, tileHeight = patch.height*SCALE;
        for (float down = 0; down < height; down += tileHeight) {
            float drawHeight = Math.min(tileHeight,height-down);
            for (float across = 0; across < width; across += tileWidth) {
                float drawWidth = Math.min(tileWidth,width-across);
                // Crop the final piece instead of stretching a whole picture into a differently shaped block.
                batch.draw(patch.texture,x+across,y+height-down-drawHeight,drawWidth,drawHeight,
                    patch.x,patch.y,Math.round(drawWidth/SCALE),Math.round(drawHeight/SCALE),false,false);
            }
        }
    }

    @Override public void dispose() {
        clearOutlines();
        for (int i = 0; i < textures.length; i++) if (textures[i] != null) { textures[i].dispose(); textures[i] = null; }
        for(int i=0;i<sources.length;i++)if(sources[i]!=null){sources[i].dispose();sources[i]=null;}
    }
}
