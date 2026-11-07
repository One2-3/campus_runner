package io.jbnu.test;

/** Pixel morphology on the completed alpha silhouette, excluding enclosed transparent holes. */
final class PlatformOutlineMask {
    static boolean[] create(boolean[] solid, int width, int height, int radius) {
        if (width <= 0 || height <= 0 || solid.length != width * height || radius < 1)
            throw new IllegalArgumentException("Invalid platform alpha mask");
        boolean[] exterior = new boolean[solid.length], outline = new boolean[solid.length];
        int[] queue = new int[solid.length];
        int tail = 0;
        for (int x=0;x<width;x++) {
            tail=enqueue(x,solid,exterior,queue,tail);
            tail=enqueue((height-1)*width+x,solid,exterior,queue,tail);
        }
        for (int y=0;y<height;y++) {
            tail=enqueue(y*width,solid,exterior,queue,tail);
            tail=enqueue(y*width+width-1,solid,exterior,queue,tail);
        }
        for (int head=0;head<tail;head++) {
            int p=queue[head],x=p%width,y=p/width;
            if(x>0)tail=enqueue(p-1,solid,exterior,queue,tail);
            if(x+1<width)tail=enqueue(p+1,solid,exterior,queue,tail);
            if(y>0)tail=enqueue(p-width,solid,exterior,queue,tail);
            if(y+1<height)tail=enqueue(p+width,solid,exterior,queue,tail);
        }
        for (int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int p=y*width+x;
            if(!exterior[p])continue;
            boolean near=false;
            for(int dy=-radius;dy<=radius&&!near;dy++)for(int dx=-radius;dx<=radius;dx++) {
                int nx=x+dx,ny=y+dy;
                if(nx>=0&&nx<width&&ny>=0&&ny<height&&solid[ny*width+nx]){near=true;break;}
            }
            outline[p]=near;
        }
        return outline;
    }
    private static int enqueue(int p,boolean[] solid,boolean[] exterior,int[] queue,int tail) {
        if(!solid[p]&&!exterior[p]){exterior[p]=true;queue[tail++]=p;}
        return tail;
    }
}
