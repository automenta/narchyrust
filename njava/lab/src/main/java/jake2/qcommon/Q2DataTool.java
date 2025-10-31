/*
 * Q2DataDialog.java
 * Copyright (C)  2003
 */
package jake2.qcommon;

import jake2.Jake2;
import jcog.data.list.Lst;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Q2DataTool {
    private static final String home = System.getProperty("user.home");
    private static final String sep = System.getProperty("file.separator");
    private static final String dataDir = home + sep + "Jake2";
    private static final String baseq2Dir = dataDir + sep + "baseq2";

    private final List<String> mirrorNames = new Lst<>();
    private final List<String> mirrorLinks = new Lst<>();
    private final byte[] buf = new byte[64*1024];

    public void testQ2Data() {
        initMirrors();
        for(int i=0; !isAvail() && i<mirrorNames.size(); i++) {
            try {
                install(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void dispose() {
    }
    
    static void setStatus(String text) {
        System.err.println(text);
        System.err.println();
    }

    private static boolean isAvail() {
        Cvar.Set("cddir", baseq2Dir);
        FS.setCDDir();
        return null != FS.LoadFile("pics/colormap.pcx");        
    }

    private void initMirrors() {
        InputStream in = Jake2.class.getResourceAsStream("mirrors");
        try (in; BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            while (true) {
                String name = r.readLine();
                String value = r.readLine();
                if (name == null || value == null) break;
                mirrorNames.add(name);
                mirrorLinks.add(value);
            }
        } catch (Exception e) {
        }
    }

    private void install(int mirrorIdx) {
        String mirrorName = mirrorNames.get(mirrorIdx);
        String mirror = mirrorLinks.get(mirrorIdx);

        setStatus("downloading from "+mirrorName+": <"+mirror+ '>');

        File dir = null;
        try {
            dir = new File(dataDir);
            dir.mkdirs();
        }
        catch (Exception e) {}
        try {
            if (!dir.isDirectory() || !dir.canWrite()) {
                setStatus("can't write to " + dataDir);
                return;
            } 
        }
        catch (Exception e) {
            setStatus(e.getMessage());
            return;
        }

        File outFile;
        OutputStream out = null;
        InputStream in = null;
        try {
            URL url = new URL(mirror);
            URLConnection conn = url.openConnection();
            

            in = conn.getInputStream();

            outFile = File.createTempFile("Jake2Data", ".zip");
            outFile.deleteOnExit();
            out = new FileOutputStream(outFile);

            copyStream(in, out);
        } catch (Exception e) {
            setStatus(e.getMessage());
            return;
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
            try {
                out.close();
            } catch (Exception e) {}                
        }

        try {
            installData(outFile.getCanonicalPath());
        } catch (Exception e) {
            setStatus(e.getMessage());
            return;
        }


        try {
            if (outFile != null) outFile.delete();
        } catch (Exception e) {}

        setStatus("installation successful from "+mirrorName+": <"+mirror+ '>');
    }


    private void installData(String filename) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            ZipFile f = new ZipFile(filename);
            Enumeration<? extends ZipEntry> e = f.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                String name = entry.getName();
                int i;
                if ((i = name.indexOf("/baseq2")) > -1 && !name.contains(".dll")) {
                    name = dataDir + name.substring(i);
                    File outFile = new File(name);
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        setStatus("installing " + outFile.getName());
                        outFile.getParentFile().mkdirs();
                        out = new FileOutputStream(outFile);
                        in = f.getInputStream(entry);
                        copyStream(in, out);
                    }
                }
            }
        } finally {
            try {in.close();} catch (Exception e1) {}
            try {out.close();} catch (Exception e1) {}                              
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        try (in; out) {

            int l;
            while ((l = in.read(buf)) > 0) {
                out.write(buf, 0, l);

            }
        }
    }

}
