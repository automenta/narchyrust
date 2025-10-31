//package spacegraph.slam;
//
//import boofcv.alg.geo.PerspectiveOps;
//import boofcv.gui.BoofSwingUtil;
//import boofcv.struct.Point3dRgbI_F64;
//import boofcv.visualize.PointCloudViewer;
//import georegression.struct.ConvertFloatType;
//import georegression.struct.point.Point3D_F64;
//import georegression.struct.se.Se3_F32;
//import georegression.struct.se.Se3_F64;
//import georegression.struct.se.SpecialEuclideanOps_F64;
//import org.ddogleg.struct.DogArray;
//import org.ddogleg.struct.DogArray_F32;
//import org.ddogleg.struct.DogArray_I32;
//
//import javax.swing.*;
//import java.util.List;
//
//public class PointCloudViewerSwing implements PointCloudViewer {
//    PointCloudViewerPanelSwing panel = new PointCloudViewerPanelSwing(1.0F);
//
//    Se3_F64 view = new Se3_F64();
//
//    public PointCloudViewerSwing() {
//    }
//
//    public void setShowAxis(boolean show) {
//    }
//
//    public void setTranslationStep(double step) {
////        BoofSwingUtil.invokeNowOrLater(() -> {
//            this.panel.setStepSize((float)step);
////        });
//    }
//
//    public void setDotSize(int pixels) {
////        BoofSwingUtil.invokeNowOrLater(() -> {
//            this.panel.setDotRadius(pixels);
////        });
//    }
//
//    public void setClipDistance(double distance) {
//        BoofSwingUtil.invokeNowOrLater(() -> {
//            this.panel.maxRenderDistance = (float)distance;
//        });
//    }
//
//    public void setFog(boolean active) {
//        BoofSwingUtil.invokeNowOrLater(() -> {
//            this.panel.fog = active;
//        });
//    }
//
//    public void setBackgroundColor(int rgb) {
//        BoofSwingUtil.invokeNowOrLater(() -> {
//            this.panel.backgroundColor = rgb;
//        });
//    }
//
//    public void addCloud(List<Point3D_F64> cloudXyz, int[] colorsRgb) {
//        if (cloudXyz.size() > colorsRgb.length) {
//            throw new IllegalArgumentException("Number of points do not match");
//        } else {
////            if (SwingUtilities.isEventDispatchThread()) {
//                for(int i = 0; i < cloudXyz.size(); ++i) {
//                    Point3D_F64 p = cloudXyz.get(i);
//                    this.panel.addPoint((float)p.x, (float)p.y, (float)p.z, colorsRgb[i]);
//                }
////            } else {
//////                SwingUtilities.invokeLater(() -> {
////                    for(int i = 0; i < cloudXyz.size(); ++i) {
////                        Point3D_F64 p = cloudXyz.get(i);
////                        this.panel.addPoint((float)p.x, (float)p.y, (float)p.z, colorsRgb[i]);
////                    }
////
////                });
////            }
//
//        }
//    }
//
//    public void addCloud(List<Point3D_F64> cloud) {
////        if (SwingUtilities.isEventDispatchThread()) {
//            for(int i = 0; i < cloud.size(); ++i) {
//                Point3D_F64 p = cloud.get(i);
//                this.panel.addPoint((float)p.x, (float)p.y, (float)p.z, 16711680);
//            }
////        } else {
////            SwingUtilities.invokeLater(() -> {
////                for(int i = 0; i < cloud.size(); ++i) {
////                    Point3D_F64 p = cloud.get(i);
////                    this.panel.addPoint((float)p.x, (float)p.y, (float)p.z, 16711680);
////                }
////
////            });
////        }
//
//    }
//
//    public void addCloud(DogArray_F32 cloudXYZ, DogArray_I32 colorRGB) {
//        if (cloudXYZ.size / 3 != colorRGB.size) {
//            throw new IllegalArgumentException("Number of points do not match");
//        } else {
////            if (SwingUtilities.isEventDispatchThread()) {
//                this.panel.addPoints(cloudXYZ.data, colorRGB.data, colorRGB.size);
////            } else {
////                SwingUtilities.invokeLater(() -> {
////                    this.panel.addPoints(cloudXYZ.data, colorRGB.data, cloudXYZ.size / 3);
////                });
////            }
//
//        }
//    }
//
//    public void addPoint(double x, double y, double z, int rgb) {
////        if (SwingUtilities.isEventDispatchThread()) {
//            this.panel.addPoint((float)x, (float)y, (float)z, rgb);
////        } else {
////            SwingUtilities.invokeLater(() -> {
////                this.panel.addPoint((float)x, (float)y, (float)z, rgb);
////            });
////        }
//
//    }
//
//    public void addWireFrame(List<Point3D_F64> vertexes, boolean closed, int rgb, int widthPixels) {
//        this.panel.addWireFrame(vertexes, closed, rgb, widthPixels);
//    }
//
//    public void clearPoints() {
//        this.panel.clearCloud();
//
//    }
//
//    public void setColorizer(Colorizer colorizer) {
//        this.panel.colorizer = colorizer;
//    }
//
//    public void removeColorizer() {
//        this.panel.colorizer = null;
//    }
//
//    public void setCameraHFov(double radians) {
//        this.panel.setHorizontalFieldOfView((float)radians);
//    }
//
//    public void setCameraToWorld(Se3_F64 cameraToWorld) {
//        view = cameraToWorld;
//        Se3_F64 worldToCamera = cameraToWorld.invert(null);
//        Se3_F32 worldToCameraF32 = new Se3_F32();
//        ConvertFloatType.convert(worldToCamera, worldToCameraF32);
//
//        this.panel.setWorldToCamera(worldToCameraF32);
//    }
//
//    public Se3_F64 getCameraToWorld(Se3_F64 cameraToWorld) {
//        if (cameraToWorld == null) {
//            cameraToWorld = new Se3_F64();
//        }
//
//        Se3_F32 worldToCamera = this.panel.getWorldToCamera(null);
//        ConvertFloatType.convert(worldToCamera.invert(null), cameraToWorld);
//        return cameraToWorld;
//    }
//
//    public DogArray<Point3dRgbI_F64> copyCloud(DogArray<Point3dRgbI_F64> copy) {
//        if (copy == null) {
//            copy = new DogArray(Point3dRgbI_F64::new);
//        } else {
//            copy.reset();
//        }
//
//        var cloudXyz = this.panel.cloudXyz;
//        var cloudColor = this.panel.cloudColor;
//        synchronized(cloudXyz) {
//            int N = cloudXyz.size() / 3;
//            int idxXyz;
//            int i;
//            Point3dRgbI_F64 p;
//            if (N == cloudColor.size()) {
//                idxXyz = 0;
//
//                for(i = 0; i < N; ++i) {
//                    p = copy.grow();
//                    p.x = cloudXyz.get(idxXyz++);
//                    p.y = cloudXyz.get(idxXyz++);
//                    p.z = cloudXyz.get(idxXyz++);
//                    p.rgb = cloudColor.get(i);
//                }
//            } else {
//                idxXyz = 0;
//
//                for(i = 0; i < N; ++i) {
//                    p = copy.grow();
//                    p.x = cloudXyz.get(idxXyz++);
//                    p.y = cloudXyz.get(idxXyz++);
//                    p.z = cloudXyz.get(idxXyz++);
//                    p.rgb = 0;
//                }
//            }
//
//            return copy;
//        }
//    }
//
//    public JComponent getComponent() {
//        return this.panel;
//    }
//
//    public int size() {
//        return this.panel.cloudColor.size();
//    }
//
//    public void addCloud(ExampleTrifocalStereoUncalibrated.PointCloudResult r) {
//        final double hfov = PerspectiveOps.computeHFov(PerspectiveOps.matrixToPinhole(r.rectifiedK, r.w, r.h, null));
//
//        synchronized(this) {
//            System.out.println(hfov);
//
//            if (hfov == hfov) {
//                setTranslationStep(r.baseline/10);
//                if (this.panel.hfov == 0) setCameraHFov(hfov);
//                else setCameraHFov(Math.min(this.panel.hfov, hfov));
//                //setCameraHFov(hfov);
//
//
//                // skew the view to make the structure easier to see
//                Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(-r.baseline * 5, 0, 0, 0, 0.2, 0, null);
//                setCameraToWorld(cameraToWorld);
//
//                addCloud(r.cloud.cloudXyz, r.cloud.cloudRgb);
//            }
//        }
//    }
//}