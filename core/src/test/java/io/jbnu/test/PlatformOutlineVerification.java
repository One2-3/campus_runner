package io.jbnu.test;

/** Alpha-mask checks independent of GL: jagged silhouette, merged tiles, enclosed holes and thickness. */
public final class PlatformOutlineVerification {
    public static void main(String[] args) {
        boolean[] silhouette=new boolean[81];
        silhouette[2*9+2]=true;silhouette[3*9+2]=true;silhouette[3*9+3]=true;
        boolean[] jagged=PlatformOutlineMask.create(silhouette,9,9,1);
        check(jagged[2*9+3],"Transparent notch inside bounding rectangle follows actual alpha");
        check(!jagged[1*9+4],"Does not outline empty corner of bounding rectangle");
        for(int i=0;i<81;i++)check(!(jagged[i]&&silhouette[i]),"Outline never covers source art");

        boolean[] joined=new boolean[81];
        // Adjacent opaque patches become one completed silhouette, not separate tiles.
        for(int y=2;y<=6;y++)for(int x=2;x<=6;x++)joined[y*9+x]=true;
        joined[4*9+4]=false; // Enclosed decorative alpha hole must remain untouched.
        boolean[] one=PlatformOutlineMask.create(joined,9,9,1);
        check(!one[4*9+4],"No stroke in enclosed decoration hole");
        check(!one[3*9+4]&&!one[3*9+5],"No stroke at internal tile joins");
        check(count(one)==24,"One-pixel outer ring only");
        check(count(PlatformOutlineMask.create(joined,9,9,2))==56,"Two-pixel thickness is explicit and bounded");
        check(count(PlatformOutlineMask.create(new boolean[81],9,9,1))==0,"Empty alpha produces no rectangle outline");
        System.out.println("Platform outline passed: alpha not bounding box, exterior only, no tile seams/inner holes, one/two pixel rings.");
    }
    private static int count(boolean[] mask){int count=0;for(boolean p:mask)if(p)count++;return count;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
