///*
// * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package spacegraph.slam;
//
//import boofcv.abst.disparity.StereoDisparity;
//import boofcv.abst.feature.associate.AssociateDescription;
//import boofcv.abst.feature.associate.ScoreAssociation;
//import boofcv.abst.feature.detdesc.DetectDescribePoint;
//import boofcv.abst.feature.detect.interest.ConfigFastHessian;
//import boofcv.abst.geo.Estimate1ofTrifocalTensor;
//import boofcv.abst.geo.RefineThreeViewProjective;
//import boofcv.abst.geo.bundle.BundleAdjustment;
//import boofcv.abst.geo.bundle.PruneStructureFromSceneMetric;
//import boofcv.abst.geo.bundle.SceneObservations;
//import boofcv.abst.geo.bundle.SceneStructureMetric;
//import boofcv.alg.cloud.DisparityToColorPointCloud;
//import boofcv.alg.cloud.PointCloudWriter;
//import boofcv.alg.descriptor.UtilFeature;
//import boofcv.alg.distort.ImageDistort;
//import boofcv.alg.feature.associate.AssociateThreeByPairs;
//import boofcv.alg.geo.*;
//import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
//import boofcv.alg.geo.rectify.RectifyCalibrated;
//import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
//import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
//import boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene;
//import boofcv.core.image.ConvertImage;
//import boofcv.factory.disparity.ConfigDisparityBMBest5;
//import boofcv.factory.disparity.DisparityError;
//import boofcv.factory.disparity.FactoryStereoDisparity;
//import boofcv.factory.feature.associate.ConfigAssociateGreedy;
//import boofcv.factory.feature.associate.FactoryAssociation;
//import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
//import boofcv.factory.geo.*;
//import boofcv.io.image.ConvertBufferedImage;
//import boofcv.io.image.UtilImageIO;
//import boofcv.struct.border.BorderType;
//import boofcv.struct.calib.CameraPinhole;
//import boofcv.struct.calib.CameraPinholeBrown;
//import boofcv.struct.distort.DoNothing2Transform2_F64;
//import boofcv.struct.feature.AssociatedTripleIndex;
//import boofcv.struct.feature.TupleDesc_F64;
//import boofcv.struct.geo.AssociatedTriple;
//import boofcv.struct.geo.TrifocalTensor;
//import boofcv.struct.image.*;
//import georegression.struct.point.Point2D_F64;
//import georegression.struct.point.Point3D_F64;
//import georegression.struct.point.Vector3D_F64;
//import georegression.struct.se.Se3_F64;
//import jcog.data.list.Lst;
//import org.ddogleg.fitting.modelset.ransac.Ransac;
//import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
//import org.ddogleg.struct.DogArray;
//import org.ddogleg.struct.DogArray_I32;
//import org.ddogleg.struct.FastAccess;
//import org.ejml.data.DMatrixRMaj;
//import org.ejml.data.FMatrixRMaj;
//import org.ejml.dense.row.CommonOps_DDRM;
//import org.ejml.ops.ConvertMatrixData;
//
//import java.awt.image.BufferedImage;
//import java.util.ArrayList;
//import java.util.List;
//
//import static boofcv.alg.geo.MultiViewOps.triangulatePoints;
////import static boofcv.examples.stereo.ExampleStereoTwoViewsOneCamera.rectifyImages;
////import static boofcv.examples.stereo.ExampleStereoTwoViewsOneCamera.showPointCloud;
//
///**
// * In this example three uncalibrated images are used to compute a point cloud. Extrinsic as well as all intrinsic
// * parameters (e.g. focal length and lens distortion) are found. Stereo disparity is computed between two of
// * the three views and the point cloud derived from that. To keep the code (relatively) simple, extra steps which
// * improve convergence have been omitted. See {@link ThreeViewEstimateMetricScene} for
// * a more robust version of what has been presented here. Even with these simplifications this example can be
// * difficult to fully understand.
// *
// * Three images produce a more stable "practical" algorithm when dealing with uncalibrated images.
// * With just two views its impossible to remove all false matches since an image feature can lie any where
// * along an epipolar line in other other view. Even with three views, results are not always stable or 100% accurate
// * due to scene geometry and here the views were captured. In general you want a well textured scene with objects
// * up close and far away, and images taken with translational
// * motion. Pure rotation and planar scenes are impossible to estimate the structure from.
// *
// * Steps:
// * <ol>
// *     <li>Feature Detection (e.g. SURF)</li>
// *     <li>Two view association</li>
// *     <li>Find 3 View Tracks</li>
// *     <li>Fit Trifocal tensor using RANSAC</li>
// *     <li>Get and refine camera matrices</li>
// *     <li>Compute dual absolute quadratic</li>
// *     <li>Estimate intrinsic parameters from DAC</li>
// *     <li>Estimate metric scene structure</li>
// *     <li>Sparse bundle adjustment</li>
// *     <li>Rectify two of the images</li>
// *     <li>Compute stereo disparity</li>
// *     <li>Convert into a point cloud</li>
// * </ol>
// *
// * For a more stable and accurate version this example see {@link ThreeViewEstimateMetricScene}.
// *
// * @author Peter Abeles
// */
//public class ExampleTrifocalStereoUncalibrated {
//	public static void main( String[] args ) {
////		String name = "rock_leaves_";
////		String name = "mono_wall_";
//		String name = "minecraft_cave1_";
////		String name = "minecraft_distant_";
////		String name = "bobcats_";
////		String name = "chicken_";
////		String name = "turkey_";
////		String name = "rockview_";
////		String name = "pebbles_";
////		String name = "books_";
////		String name = "skull_";
////		String name = "triflowers_";
//		pointCloud(
//				UtilImageIO.loadImage("file://tmp/t", name + "01.jpg"),
//				UtilImageIO.loadImage("file://tmp/t", name + "02.jpg"),
//				UtilImageIO.loadImage("file://tmp/t", name + "03.jpg")
//		);
////		for (int i = 1; i < 8; i++) {
////			try {
////				pointCloud(
////						UtilImageIO.loadImage("file://tmp/p", "m43" + i + ".jpeg"),
////						UtilImageIO.loadImage("file://tmp/p", "m43" + (i + 1) + ".jpeg"),
////						UtilImageIO.loadImage("file://tmp/p", "m43" + (i + 2) + ".jpeg")
////				);
////			} catch (Throwable t) {
////				System.out.println(t);
////			}
////		}
//	}
//
//	public static Object pointCloud(BufferedImage buff01, BufferedImage buff02, BufferedImage buff03) {
//		Planar<GrayU8> color01 = ConvertBufferedImage.convertFrom(buff01, true, ImageType.pl(3, GrayU8.class));
//		Planar<GrayU8> color02 = ConvertBufferedImage.convertFrom(buff02, true, ImageType.pl(3, GrayU8.class));
//		Planar<GrayU8> color03 = ConvertBufferedImage.convertFrom(buff03, true, ImageType.pl(3, GrayU8.class));
//
//		GrayU8 image01 = ConvertImage.average(color01, null);
//		GrayU8 image02 = ConvertImage.average(color02, null);
//		GrayU8 image03 = ConvertImage.average(color03, null);
//
//		// using SURF features. Robust and fairly fast to compute
//		DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc = FactoryDetectDescribe.surfStable(
//			new ConfigFastHessian(0, 4, 1000, 1, 9, 4, 2), null, null, GrayU8.class);
//
//		DogArray<Point2D_F64> locations01 = new DogArray<>(Point2D_F64::new);
//		DogArray<Point2D_F64> locations02 = new DogArray<>(Point2D_F64::new);
//		DogArray<Point2D_F64> locations03 = new DogArray<>(Point2D_F64::new);
//
//		DogArray<TupleDesc_F64> features01 = UtilFeature.createQueue(detDesc, 100);
//		DogArray<TupleDesc_F64> features02 = UtilFeature.createQueue(detDesc, 100);
//		DogArray<TupleDesc_F64> features03 = UtilFeature.createQueue(detDesc, 100);
//		DogArray_I32 featureSet01 = new DogArray_I32();
//		DogArray_I32 featureSet02 = new DogArray_I32();
//		DogArray_I32 featureSet03 = new DogArray_I32();
//
//		// Converting data formats for the found features into what can be processed by SFM algorithms
//		// Notice how the image center is subtracted from the coordinates? In many cases a principle point
//		// of zero is assumed. This is a reasonable assumption in almost all modern cameras. Errors in
//		// the principle point tend to materialize as translations and are non fatal.
//
//		int width = image01.width, height = image01.height;
////		System.out.println("Image Shape " + width + " x " + height);
//		double cx = width/2;
//		double cy = height/2;
//
//		detDesc.detect(image01);
//		extractFeatures(detDesc, locations01, features01, featureSet01, cx, cy);
//
//		detDesc.detect(image02);
//		extractFeatures(detDesc, locations02, features02, featureSet02, cx, cy);
//
//		detDesc.detect(image03);
//		extractFeatures(detDesc, locations03, features03, featureSet03, cx, cy);
//
////		System.out.println("features01.size = " + features01.size);
////		System.out.println("features02.size = " + features02.size);
////		System.out.println("features03.size = " + features03.size);
//
//		ScoreAssociation<TupleDesc_F64> scorer = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
//		AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 0.1), scorer);
//
//		AssociateThreeByPairs<TupleDesc_F64> associateThree = new AssociateThreeByPairs<>(associate, TupleDesc_F64.class);
//
//		associateThree.initialize(detDesc.getNumberOfSets());
//		associateThree.setFeaturesA(features01, featureSet01);
//		associateThree.setFeaturesB(features02, featureSet02);
//		associateThree.setFeaturesC(features03, featureSet03);
//
//		associateThree.associate();
//
////		System.out.println("Total Matched Triples = " + associateThree.getMatches().size);
//
//		ConfigRansac configRansac = new ConfigRansac();
//		configRansac.iterations = 500;
//		configRansac.inlierThreshold = 1;
//
//		ConfigTrifocal configTri = new ConfigTrifocal();
//		// estimate using all the inliers
//		// No need to re-scale the input because the estimator automatically adjusts the input on its own
//		configTri.which = EnumTrifocal.ALGEBRAIC_7;
//		configTri.converge.maxIterations = 100;
//
//		ConfigTrifocalError configError = new ConfigTrifocalError();
//		configError.model = ConfigTrifocalError.Model.REPROJECTION_REFINE;
//
//
//		Ransac<TrifocalTensor, AssociatedTriple> ransac =
//				FactoryMultiViewRobust.trifocalRansac(configTri, configError, configRansac);
//
//		// Create and configure the bundle adjustment solver
//		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
//		configLM.dampeningInitial = 1e-3;
//		configLM.hessianScaling = false;
//		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
//		configSBA.configOptimizer = configLM;
//
//		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
//		// prints out useful debugging information that lets you know how well it's converging
////		bundleAdjustment.setVerbose(System.out,0);
//		bundleAdjustment.configure(1e-6, 1e-6, 100); // convergence criteria
//
//		//-----
//
//		DogArray<AssociatedTripleIndex> associatedIdx = associateThree.getMatches();
//		DogArray<AssociatedTriple> associated = new DogArray<>(AssociatedTriple::new);
//		for (int i = 0; i < associatedIdx.size; i++) {
//			AssociatedTripleIndex p = associatedIdx.get(i);
//			associated.grow().setTo(locations01.get(p.a), locations02.get(p.b), locations03.get(p.c));
//		}
//		ransac.process(associated.toList());
//
//		List<AssociatedTriple> inliers = ransac.getMatchSet();
//		TrifocalTensor model = ransac.getModelParameters();
////		System.out.println("Remaining after RANSAC " + inliers.size());
//
//		// Show remaining associations from RANSAC
////		AssociatedTriplePanel triplePanel = new AssociatedTriplePanel();
////		triplePanel.setPixelOffset(cx, cy);
////		triplePanel.setImages(buff01, buff02, buff03);
////		triplePanel.setAssociation(inliers);
////		ShowImages.showWindow(triplePanel, "Associations", true);
//
//		Estimate1ofTrifocalTensor trifocalEstimator = FactoryMultiView.trifocal_1(configTri);
//		if (!trifocalEstimator.process(inliers, model))
//			throw new RuntimeException("Estimator failed");
////		model.print();
//
//		DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);
//		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
//		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);
//		MultiViewOps.trifocalCameraMatrices(model, P2, P3);
//
//		// Most of the time this refinement step makes little difference, but in some edges cases it appears
//		// to help convergence
////		System.out.println("Refining projective camera matrices");
//		RefineThreeViewProjective refineP23 = FactoryMultiView.threeViewRefine(null);
//		if (!refineP23.process(inliers, P2, P3, P2, P3))
//			throw new RuntimeException("Can't refine P2 and P3!");
//
//
//		SelfCalibrationLinearDualQuadratic selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
//		selfcalib.addCameraMatrix(P1);
//		selfcalib.addCameraMatrix(P2);
//		selfcalib.addCameraMatrix(P3);
//
//		List<CameraPinhole> pinholes = new ArrayList<>();
//		GeometricResult result = selfcalib.solve();
//		if (GeometricResult.SOLVE_FAILED != result) {
//			FastAccess<Intrinsic> s = selfcalib.getSolutions();
//			int sn = s.size;
//			for (int i = 0; i < sn; i++) {
//				Intrinsic c = s.get(i);
//				pinholes.add(new CameraPinhole(c.fx, c.fy, 0, 0, 0, width, height));
//			}
//		} else {
//			System.out.println("Self calibration failed!");
//			for (int i = 0; i < 3; i++) {
//				pinholes.add(new CameraPinhole(width/2, height/2, 0, 0, 0, width, height));
//			}
//		}
//
////		// print the initial guess for focal length. Focal length is a crtical and difficult to estimate
////		// parameter
////		for (int i = 0; i < 3; i++) {
////			CameraPinhole r = pinholes.get(i);
////			System.out.println("fx=" + r.fx + " fy=" + r.fy + " skew=" + r.skew);
////		}
//
////		System.out.println("Projective to metric");
//		// convert camera matrix from projective to metric
//		DMatrixRMaj H = new DMatrixRMaj(4, 4); // storage for rectifying homography
//		if (!MultiViewOps.absoluteQuadraticToH(selfcalib.getQ(), H))
//			throw new RuntimeException("Projective to metric failed");
//
//		DMatrixRMaj K = new DMatrixRMaj(3, 3);
//		Lst<Se3_F64> worldToView = new Lst<>();
//		for (int i = 0; i < 3; i++)
//			worldToView.add(new Se3_F64());
//
//		// ignore K since we already have that
//		MultiViewOps.projectiveToMetric(P1, H, worldToView.get(0), K);
//		MultiViewOps.projectiveToMetric(P2, H, worldToView.get(1), K);
//		MultiViewOps.projectiveToMetric(P3, H, worldToView.get(2), K);
//
//		// scale is arbitrary. Set max translation to 1
//		adjustTranslationScale(worldToView);
//
//		// Construct bundle adjustment data structure
//		SceneStructureMetric structure = new SceneStructureMetric(false);
//		int in = inliers.size();
//		structure.initialize(3, 3, in);
//
//		SceneObservations observations = new SceneObservations();
//		observations.initialize(3);
//
//		int pn = pinholes.size();
//		for (int i = 0; i < pn; i++) {
//			BundlePinholeSimplified bp = new BundlePinholeSimplified();
//			bp.f = pinholes.get(i).fx;
//			structure.setCamera(i, false, bp);
//			structure.setView(i, i, i == 0, worldToView.get(i));
//		}
//		for (int i = 0; i < in; i++) {
//			AssociatedTriple t = inliers.get(i);
//
//			observations.getView(0).add(i, (float)t.p1.x, (float)t.p1.y);
//			observations.getView(1).add(i, (float)t.p2.x, (float)t.p2.y);
//			observations.getView(2).add(i, (float)t.p3.x, (float)t.p3.y);
//
//			structure.connectPointToView(i, 0);
//			structure.connectPointToView(i, 1);
//			structure.connectPointToView(i, 2);
//		}
//
//		// Initial estimate for point 3D locations
//		triangulatePoints(structure, observations);
//
//
//
//
//		bundleAdjustment.setParameters(structure, observations);
//		bundleAdjustment.optimize(structure);
//
//		// See if the solution is physically possible. If not fix and run bundle adjustment again
//		checkBehindCamera(structure, observations, bundleAdjustment);
//
//		// It's very difficult to find the best solution due to the number of local minimum. In the three view
//		// case it's often the problem that a small translation is virtually identical to a small rotation.
//		// Convergence can be improved by considering that possibility
//
//		// Now that we have a decent solution, prune the worst outliers to improve the fit quality even more
//		PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure, observations);
//		pruner.pruneObservationsByErrorRank(0.7);
//		pruner.pruneViews(10);
//		pruner.pruneUnusedMotions();
//		pruner.prunePoints(1);
//		bundleAdjustment.setParameters(structure, observations);
//		bundleAdjustment.optimize(structure);
//
////		System.out.println("Final Views");
//		for (int i = 0; i < 3; i++) {
//			BundlePinholeSimplified cp = structure.getCameras().get(i).getModel();
//			Vector3D_F64 T = structure.getParentToView(i).T;
////			System.out.printf("[ %d ] f = %5.1f T=%s\n", i, cp.f, T.toString());
//		}
//
////		System.out.println("\n\nComputing Stereo Disparity");
//		BundlePinholeSimplified cp = structure.getCameras().get(0).getModel();
//		CameraPinholeBrown intrinsic01 = new CameraPinholeBrown();
//		intrinsic01.fsetK(cp.f, cp.f, 0, cx, cy, width, height);
//		intrinsic01.fsetRadial(cp.k1, cp.k2);
//
//		cp = structure.getCameras().get(1).getModel();
//		CameraPinholeBrown intrinsic02 = new CameraPinholeBrown();
//		intrinsic02.fsetK(cp.f, cp.f, 0, cx, cy, width, height);
//		intrinsic02.fsetRadial(cp.k1, cp.k2);
//
//		Se3_F64 leftToRight = structure.getParentToView(1);
//
//		// TODO dynamic max disparity
//		return computeStereoCloud(image01, image02, color01, color02, intrinsic01, intrinsic02, leftToRight, 0, 250);
//	}
//
//	public static void extractFeatures(DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc, DogArray<Point2D_F64> locations01, DogArray<TupleDesc_F64> features01, DogArray_I32 featureSet01, double cx, double cy) {
//		int n = detDesc.getNumberOfFeatures();
//		for (int i = 0; i < n; i++) {
//			Point2D_F64 pixel = detDesc.getLocation(i);
//			locations01.grow().setTo(pixel.x - cx, pixel.y - cy);
//			features01.grow().setTo(detDesc.getDescription(i));
//			featureSet01.add(detDesc.getSet(i));
//		}
//	}
//
//	private static void adjustTranslationScale( List<Se3_F64> worldToView ) {
//		double maxT = 0;
//		for (Se3_F64 p : worldToView) {
//			maxT = Math.max(maxT, p.T.norm());
//		}
//		for (Se3_F64 p : worldToView) {
//			p.T.scale(1.0/maxT);
//			p.print();
//		}
//	}
//
//	// TODO Do this correction without running bundle adjustment again
//	private static void checkBehindCamera( SceneStructureMetric structure, SceneObservations observations, BundleAdjustment<SceneStructureMetric> bundleAdjustment ) {
//
//		int totalBehind = 0;
//		Point3D_F64 X = new Point3D_F64();
//		for (int i = 0; i < structure.points.size; i++) {
//			if (structure.points.data[i].coordinate[2] < 0)
//				totalBehind++;
//		}
//		structure.getParentToView(1).T.print();
//		if (totalBehind > structure.points.size/2) {
//			System.out.println("Flipping because it's reversed. score = " + bundleAdjustment.getFitScore());
//			for (int i = 1; i < structure.views.size; i++) {
//				Se3_F64 w2v = structure.getParentToView(i);
//				w2v.setTo(w2v.invert(null));
//			}
//			triangulatePoints(structure, observations);
//
//			bundleAdjustment.setParameters(structure, observations);
//			bundleAdjustment.optimize(structure);
//			System.out.println("  after = " + bundleAdjustment.getFitScore());
//		} else {
//			System.out.println("Points not behind camera. " + totalBehind + " / " + structure.points.size);
//		}
//	}
//
//	public static PointCloudResult computeStereoCloud(GrayU8 distortedLeft, GrayU8 distortedRight,
//													  Planar<GrayU8> colorLeft, Planar<GrayU8> colorRight,
//													  CameraPinholeBrown intrinsicLeft,
//													  CameraPinholeBrown intrinsicRight,
//													  Se3_F64 leftToRight,
//													  int minDisparity, int rangeDisparity ) {
//
////		drawInliers(origLeft, origRight, intrinsic, inliers);
//
//		// rectify a colored image
//		Planar<GrayU8> rectLeft = colorLeft.createSameShape();
//		Planar<GrayU8> rectRight = colorLeft.createSameShape();
//		GrayU8 rectMask = new GrayU8(colorLeft.width, colorLeft.height);
//
//		PointCloudBuilder b = rectifyImages(colorLeft, colorRight, leftToRight, intrinsicLeft, intrinsicRight,
//				rectLeft, rectRight, rectMask);
//
//		return b.get(distortedLeft, distortedRight, colorLeft, colorRight, minDisparity, rangeDisparity, rectMask, leftToRight);
//	}
//	/**
//	 * Remove lens distortion and rectify stereo images
//	 *
//	 * @param distortedLeft Input distorted image from left camera.
//	 * @param distortedRight Input distorted image from right camera.
//	 * @param leftToRight Camera motion from left to right
//	 * @param intrinsicLeft Intrinsic camera parameters
//	 * @param rectifiedLeft Output rectified image for left camera.
//	 * @param rectifiedRight Output rectified image for right camera.
//	 * @param rectifiedMask Mask that indicates invalid pixels in rectified image. 1 = valid, 0 = invalid
//	 * @param rectifiedK Output camera calibration matrix for rectified camera
//	 */
//	public static <T extends ImageBase<T>> PointCloudBuilder rectifyImages( T distortedLeft,
//						T distortedRight,
//						Se3_F64 leftToRight,
//						CameraPinholeBrown intrinsicLeft,
//						CameraPinholeBrown intrinsicRight,
//						T rectifiedLeft,
//						T rectifiedRight,
//						GrayU8 rectifiedMask) {
//		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
//
//		// original camera calibration matrices
//		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsicLeft, (DMatrixRMaj)null);
//		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsicRight, (DMatrixRMaj)null);
//
//		rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);
//
//
//		var rectifiedR = rectifyAlg.getRectifiedRotation();
//
//		// New calibration matrix,
//		var rectifiedK = rectifyAlg.getCalibrationMatrix();
//		if (rectifiedK.get(0, 0) < 0) throw new RuntimeException("unrectified");
//
////		System.out.println("Rectified K");rectifiedK.print();
////		System.out.println("Rectified R"); rectifiedR.print();
//
//
//		// Adjust the rectification to make the view area more useful
//		ImageDimension rectShape = new ImageDimension();
//		// rectification matrix for each image
//		DMatrixRMaj rect1 = rectifyAlg.getUndistToRectPixels1();
//		DMatrixRMaj rect2 = rectifyAlg.getUndistToRectPixels2();
//		RectifyImageOps.fullViewLeft(intrinsicLeft, rect1, rect2, rectifiedK, rectShape);
//		RectifyImageOps.allInsideLeft(intrinsicLeft, rect1, rect2, rectifiedK, rectShape);
//		// Taking in account the relative rotation between the image axis and the baseline is important in
//		// this scenario since a person can easily hold the camera at an odd angle. If you don't adjust
//		// the rectified image size you might end up with a lot of wasted pixels and a low resolution model!
//		rectifiedLeft.reshape(rectShape.width, rectShape.height);
//		rectifiedRight.reshape(rectShape.width, rectShape.height);
//
//		// undistorted and rectify images
//		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3);
//		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);
//		ConvertMatrixData.convert(rect1, rect1_F32);
//		ConvertMatrixData.convert(rect2, rect2_F32);
//
//		// Extending the image prevents a harsh edge reducing false matches at the image border
//		// SKIP is another option, possibly a tinny bit faster, but has a harsh edge which will need to be filtered
//		ImageDistort<T, T> distortLeft =
//				RectifyDistortImageOps.rectifyImage(intrinsicLeft, rect1_F32, BorderType.EXTENDED, distortedLeft.getImageType());
//		ImageDistort<T, T> distortRight =
//				RectifyDistortImageOps.rectifyImage(intrinsicRight, rect2_F32, BorderType.EXTENDED, distortedRight.getImageType());
//
//		distortLeft.apply(distortedLeft, rectifiedLeft, rectifiedMask);
//		distortRight.apply(distortedRight, rectifiedRight);
//
//		return new PointCloudBuilder(rectifiedR, rectifiedK);
//	}
//
//	private static class PointCloudBuilder {
//		private final DMatrixRMaj rectifiedR, rectifiedK;
//
//		PointCloudBuilder(DMatrixRMaj rectifiedR, DMatrixRMaj rectifiedK) {
//			this.rectifiedR = rectifiedR;
//			this.rectifiedK = rectifiedK;
//		}
//
//		public PointCloudResult get(GrayU8 distortedLeft, GrayU8 distortedRight, Planar<GrayU8> colorLeft, Planar<GrayU8> colorRight, int minDisparity, int rangeDisparity, GrayU8 rectMask, Se3_F64 leftToRight) {
//
//
//			GrayU8 rectifiedLeft = rectify(distortedLeft, colorLeft);
//			GrayU8 rectifiedRight = rectify(distortedRight, colorRight);
//
//			// compute disparity
//			ConfigDisparityBMBest5 config = new ConfigDisparityBMBest5();
//			config.errorType = DisparityError.CENSUS;
//			config.disparityMin = minDisparity;
//			config.disparityRange = rangeDisparity;
//			config.subpixel = true;
//			config.regionRadiusX = config.regionRadiusY = 6;
//			config.validateRtoL = 1;
//			config.texture = 0.2;
//			StereoDisparity<GrayU8, GrayF32> disparityAlg =
//					FactoryStereoDisparity.blockMatchBest5(config, GrayU8.class, GrayF32.class);
//
//			// process and return the results
//			disparityAlg.process(rectifiedLeft, rectifiedRight);
//			GrayF32 disparity = disparityAlg.getDisparity();
//			RectifyImageOps.applyMask(disparity, rectMask, 0);
//
//			// show results
////		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, rangeDisparity, 0);
//
//			BufferedImage outLeft = ConvertBufferedImage.convertTo(colorLeft, null, true);
////			BufferedImage outRight = ConvertBufferedImage.convertTo(colorRight, null, true);
//
////		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification", true);
////		ShowImages.showWindow(visualized, "Disparity", true);
//
//			return showPointCloud(disparity, outLeft, leftToRight, rectifiedK, rectifiedR, minDisparity, rangeDisparity);
//		}
//
//		protected GrayU8 rectify(GrayU8 distortedLeft, Planar<GrayU8> colorLeft) {
//			GrayU8 rectifiedLeft = distortedLeft.createSameShape();
//			ConvertImage.average(colorLeft, rectifiedLeft);
//			return rectifiedLeft;
//		}
//	}
//
//	/**
//	 * Show results as a point cloud
//	 * @return
//	 */
//	public static PointCloudResult showPointCloud(ImageGray disparity, BufferedImage left,
//												  Se3_F64 motion, DMatrixRMaj rectifiedK, DMatrixRMaj rectifiedR,
//												  int disparityMin, int disparityRange ) {
//		PointCloudWriter.CloudArraysF32 cloud = new PointCloudWriter.CloudArraysF32();
//
//		double baseline = motion.getT().norm();
//		DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
//		d2c.configure(baseline, rectifiedK, rectifiedR, new DoNothing2Transform2_F64(), disparityMin, disparityRange);
//		d2c.process(disparity, wrap(left), cloud);
//
//		return new PointCloudResult(cloud, left, disparity, rectifiedK, baseline);
//	}
//
//	static class PointCloudResult {
//		final PointCloudWriter.CloudArraysF32 cloud;
//		final DMatrixRMaj rectifiedK;
//		final double baseline;
//		final BufferedImage left;
//		//final ImageGray disparity;
//		final int w, h;
//
//		PointCloudResult(PointCloudWriter.CloudArraysF32 cloud, BufferedImage left, ImageGray disparity, DMatrixRMaj rectifiedK, double baseline) {
//			this.cloud = cloud;
//			this.left = left;
//			//this.disparity = disparity;
//			this.w = disparity.getWidth(); this.h = disparity.getHeight();
//			this.rectifiedK = rectifiedK;
//			this.baseline = baseline;
//		}
//	}
//
//	private static DisparityToColorPointCloud.ColorImage wrap(BufferedImage image ) {
//		return new DisparityToColorPointCloud.ColorImage() {
//			final int w = image.getWidth();
//			final int h = image.getHeight();
//
//			@Override
//			public boolean isInBounds(int x, int y) {
//				return( x >= 0 && x < w && y >= 0 && y < h);
//			}
//
//			@Override
//			public int getRGB(int x, int y) {
//				return image.getRGB(x,y);
//			}
//		};
//	}
//}