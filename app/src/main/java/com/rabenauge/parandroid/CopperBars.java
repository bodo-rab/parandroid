/*
 * Copyright 2010-2011 bodo, eyebex, ralph, spotter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabenauge.parandroid;

import com.rabenauge.demo.*;
import java.nio.*;
import javax.microedition.khronos.opengles.GL11;

@SuppressWarnings("SameParameterValue")
public class CopperBars extends EffectManager {
    private static final float CYL_RADIUS=0.08f;
    private static final float CYL_LENGTH=5.0f;
    private static final int CYL_SIDES=20;

    private Demo demo;
    private static final float stx=0.0f, sty=-0.85f, stz=-3.0f;
    private static final float etx=0.0f, ety=0.0f, etz=0.0f;
    private static final float sry=90.0f, ery=0.0f;
    private static final float HIDE_MAX=5.0f;
    private static final float HIDE_SPEED=0.1f;
    private float hide=0.0f;

    private IntBuffer coords, normals;
    private ShortBuffer indices;
    private ByteBuffer[] colors;

    public boolean isHidden() {
        return hide==HIDE_MAX;
    }

    private static void calcCylinderGeom(float radius, float length, int sides, IntBuffer coords, IntBuffer normals) {
        length*=65536/2;

        int g=0, offset=sides*3;
        for (int i=0; i<sides; ++i) {
            // The coordinates are generated so that we are looking through the hollow cylinder.
            float angle=(float)i/sides*DemoMath.PI*2;
            float x=(float)Math.cos(angle)*65536;
            float y=(float)Math.sin(angle)*65536;

            int xc=(int)(x*radius);
            int yc=(int)(y*radius);
            int zc=(int)(length);

            coords.put(g  , xc);
            coords.put(g+1, yc);
            coords.put(g+2, zc);

            coords.put(offset+g  ,  xc);
            coords.put(offset+g+1,  yc);
            coords.put(offset+g+2, -zc);

            // No need to normalize these.
            normals.put(g  , (int)x);
            normals.put(g+1, (int)y);
            normals.put(g+2, 0);

            normals.put(offset+g  , (int)x);
            normals.put(offset+g+1, (int)y);
            normals.put(offset+g+2, 0);

            g+=3;
        }
    }

    private static void calcCylinderColors(int sides, ByteBuffer colors, int r, int g, int b) {
        int c=0, offset=sides*3;
        for (int i=0; i<sides; ++i) {
            float c0=((c%5)+1)/5.0f, c1=(7-(c%7))/7.0f;

            colors.put(c  , (byte)(c0*r));
            colors.put(c+1, (byte)(c0*g));
            colors.put(c+2, (byte)(c0*b));
            colors.put(c+3, (byte)255);

            colors.put(offset+c  , (byte)(c1*r));
            colors.put(offset+c+1, (byte)(c1*g));
            colors.put(offset+c+2, (byte)(c1*b));
            colors.put(offset+c+3, (byte)255);

            c+=4;
        }
    }

    private class UpDown extends Effect {
        public void onStart(GL11 gl) {
            gl.glEnable(GL11.GL_LIGHT0);
            gl.glEnable(GL11.GL_COLOR_MATERIAL);
            gl.glNormalPointer(GL11.GL_FIXED, 0, normals);
        }

        public void onRender(GL11 gl, long t, long e, float s) {
            if (demo.shootem) {
                if (hide<HIDE_MAX) {
                    hide+=HIDE_SPEED;
                    if (hide>HIDE_MAX) {
                        hide=HIDE_MAX;
                    }
                }
                else {
                    // Do nothing if we are in the "Shoot'em!" mode and
                    // the effect is already completely hidden.
                    return;
                }
            }
            else {
                if (hide>0.0f) {
                    hide-=HIDE_SPEED;
                    if (hide<0.0f) {
                        hide=0.0f;
                    }
                }
            }

            // Set OpenGL states.
            gl.glDisable(GL11.GL_BLEND);
            gl.glDisable(GL11.GL_TEXTURE_2D);
            gl.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

            gl.glEnable(GL11.GL_LIGHTING);
            gl.glEnable(GL11.GL_DEPTH_TEST);
            gl.glEnableClientState(GL11.GL_COLOR_ARRAY);
            gl.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            gl.glMatrixMode(GL11.GL_MODELVIEW);
            gl.glPushMatrix();

            // Start position and orientation for the bars.
            float ss=hide/HIDE_MAX;
            float tx=stx+(etx-stx)*ss;
            float ty=sty+(ety-sty)*ss;
            float tz=stz+(etz-stz)*ss;
            gl.glTranslatef(tx, ty, tz);

            float ry=sry+(ery-sry)*ss;
            gl.glRotatef(ry, 0, 1, 0);

            gl.glVertexPointer(3, GL11.GL_FIXED, 0, coords);

            for (int i=0;i<3; ++i) {
                gl.glPushMatrix();
                gl.glTranslatef(i*2*CYL_RADIUS, (float)Math.sin(t/400.0f+i)*0.25f, 0);

                gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, colors[i]);
                gl.glDrawElements(GL11.GL_TRIANGLE_STRIP, indices.capacity(), GL11.GL_UNSIGNED_SHORT, indices);

                gl.glPopMatrix();
            }

            // Restore OpenGL states.
            gl.glColor4f(1, 1, 1, 1);

            gl.glPopMatrix();

            gl.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            gl.glDisableClientState(GL11.GL_COLOR_ARRAY);
            gl.glDisable(GL11.GL_DEPTH_TEST);
            gl.glDisable(GL11.GL_LIGHTING);

            gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            gl.glEnable(GL11.GL_TEXTURE_2D);
            gl.glEnable(GL11.GL_BLEND);
        }
    }

    public CopperBars(Demo demo, GL11 gl) {
        super(gl);

        this.demo=demo;

        // Generate the cylinder geometry.
        coords=DirectBuffer.nativeIntBuffer(CYL_SIDES*3*2);
        normals=DirectBuffer.nativeIntBuffer(CYL_SIDES*3*2);
        calcCylinderGeom(CYL_RADIUS, CYL_LENGTH, CYL_SIDES, coords, normals);

        indices=DirectBuffer.nativeShortBuffer(CYL_SIDES*2+2);
        for (int i=0; i<CYL_SIDES; ++i) {
            indices.put((short)(i));
            indices.put((short)(i+CYL_SIDES));
        }
        indices.put((short)0);
        indices.put((short)CYL_SIDES);
        indices.position(0);

        // Calculate the cylinder colors.
        colors=new ByteBuffer[3];

        colors[0]=DirectBuffer.nativeByteBuffer(CYL_SIDES*4*2);
        calcCylinderColors(CYL_SIDES, colors[0], 255, 255, 255);

        colors[1]=DirectBuffer.nativeByteBuffer(CYL_SIDES*4*2);
        calcCylinderColors(CYL_SIDES, colors[1], 255, 0, 0);

        colors[2]=DirectBuffer.nativeByteBuffer(CYL_SIDES*4*2);
        calcCylinderColors(CYL_SIDES, colors[2], 143, 115, 99);

        // Schedule the effects in this part.
        add(new UpDown(), Demo.DURATION_MAIN_EFFECTS);
    }
}
