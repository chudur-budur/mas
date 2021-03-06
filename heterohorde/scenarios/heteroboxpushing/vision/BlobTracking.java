/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */
package sim.app.horde.scenarios.heteroboxpushing.vision; 


public class BlobTracking {
    private long swigCPtr;
    protected boolean swigCMemOwn;

    public BlobTracking(long cPtr, boolean cMemoryOwn) {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
        }

    public static long getCPtr(BlobTracking obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
        }

    protected void finalize() {
        delete();
        }

    public synchronized void delete() {
        if (swigCPtr != 0) {
            if (swigCMemOwn) {
                swigCMemOwn = false;
                blobJNI.delete_BlobTracking(swigCPtr);
                }
            swigCPtr = 0;
            }
        }

    public BlobTracking(boolean display) {
        this(blobJNI.new_BlobTracking__SWIG_0(display), true);
        }

    public BlobTracking() {
        this(blobJNI.new_BlobTracking__SWIG_1(), true);
        }

    public int getBiggestBlob() {
        return blobJNI.BlobTracking_getBiggestBlob(swigCPtr, this);
        }

    public void display() {
        blobJNI.BlobTracking_display(swigCPtr, this);
        }

    public void update() {
        blobJNI.BlobTracking_update(swigCPtr, this);
        }

    public boolean isVisible() {
        return blobJNI.BlobTracking_isVisible(swigCPtr, this);
        }

    }
