package nars.io;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import nars.$;
import nars.NAR;
import spacegraph.video.VideoSource;
import spacegraph.video.WebCam;


public class Video extends Device {

    public VideoSource c;

    public Video(Webcam cam) throws WebcamException {
        this(new WebCam(cam));
    }

    public Video(WebCam cam) {
        super($.p($.atomic("video"), $.atomic(cam.webcam.getName())));
        c = cam;
//        try {
//            serialize("/tmp/x." + this.hashCode() + ".mp4"); //TEMPORARY
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    @Override
    protected void stopping(NAR nar) {
        try {
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public Off serialize(String path)  {
//
//        return c.tensor.on(new Consumer<RGBBufImgBitmap2D>() {
//            private IMuxer muxer;
//            //Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
//            //Robot robot = new Robot();
//            static final IVelvetVideoLib lib = VelvetVideoLib.getInstance();
//
//            int FRAMERATE = 25;
//
//            final IVideoEncoderBuilder encoder = lib.videoEncoder("libx264");
//
////                TODO Picture p = new Picture()
//
//            final AtomicBoolean busy = new AtomicBoolean(false);
//
//            {
////                Runtime.getRuntime().addShutdownHook(new Thread(()->{
////                    try {
////                        busy.set(true);
////                        synchronized(e) {
////                            e.finish();
////                        }
////                        NIOUtils.closeQuietly(out);
////                    } catch (Exception ioException) {
////                        ioException.printStackTrace();
////                    }
////                }));
//            }
//
//
//
//            @Override public void accept(RGBBufImgBitmap2D f) {
//                if (busy.compareAndSet(false, true)) {
//                    try {
//
//                        var i = f.image;
//
//                        //TODO also recreate encoder if image dimension changed
//                        if (muxer == null) {
//                            muxer = lib.muxer("mp4").
//                                videoEncoder(encoder
//                                        .framerate(FRAMERATE)
//                                        .dimensions(i.getWidth(), i.getHeight())
//                                        .bitrate(1000000)).
//                                build(new File(path));
//                        }
//
//                        IVideoEncoderStream videoEncoder = muxer.videoEncoder(0);
//                        videoEncoder.encode(i);
//                    } finally {
//                        busy.set(false);
//                    }
//                }
//            }
//        });
//
//    }

//    /**
//     * mp4 sequence
//     */
//    public Off _serialize(String path) throws Exception {
////        Cmd cmd = parseArguments(args, FLAGS);
////        if (cmd.argsLength() < 1) {
////            printHelpArgs(FLAGS, new String[]{"output file name"});
////            return;
////        }
//
//        ///FileChannelWrapper out;
//            //out = NIOUtils.writableChannel(new File(path));
//
//            //MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(out);
////            BufferH264ES es = new BufferH264ES(NIOUtils.mapFile(f));
//
//            FileChannelWrapper out = NIOUtils.writableChannel(new File(path));
//            SequenceEncoder e;
//            e = new SequenceEncoder(out, Rational.R(25, 1),
//                    Format.MOV,
//                    Codec.H264,
//                    (Codec) null
//            );
//
//            return c.tensor.on(new Consumer<RGBBufImgBitmap2D>() {
//
////                TODO Picture p = new Picture()
//
//                final AtomicBoolean busy = new AtomicBoolean(false);
//
//                {
//                    Runtime.getRuntime().addShutdownHook(new Thread(()->{
//                        try {
//                            busy.set(true);
//                            synchronized(e) {
//                                e.finish();
//                            }
//                            NIOUtils.closeQuietly(out);
//                        } catch (Exception ioException) {
//                            ioException.printStackTrace();
//                        }
//                    }));
//                }
//
//
//                Picture img = null;
//
//                @Override public void accept(RGBBufImgBitmap2D f) {
//                    if (busy.compareAndSet(false, true)) {
//                        try {
//                            if (img == null)
//                                img = AWTUtil.fromBufferedImageRGB(f.image);
//                            else {
//                                AWTUtil.fromBufferedImage(f.image, img);
//                            }
//
//                            synchronized(e) {
//                                e.encodeNativeFrame(img);
//                            }
//                        } catch (IOException ioException) {
//                            ioException.printStackTrace();
//                        } finally {
//                            busy.set(false);
//                        }
//                    }
//                }
//            });
//
//    }

}