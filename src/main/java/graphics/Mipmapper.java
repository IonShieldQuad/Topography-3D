package graphics;

import graphics.TextureUtils.Filtering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static graphics.TextureUtils.interpolate;

public class Mipmapper {
    private List<List<BufferedImage>> data = new ArrayList<>();
    private BufferedImage texture;
    private boolean generated = false;
    private static final int THREADS_MAX = 4;
    
    public Mipmapper() {
        data.add(new ArrayList<>());
    }
    
    public Mipmapper(BufferedImage texture) {
        loadTexture(texture);
    }
    
    public void loadTexture(BufferedImage texture) {
        generated = false;
        this.texture = texture;
        data.clear();
        data.add(new ArrayList<>());
        data.get(0).add(texture);
    }
    
    public BufferedImage getTexture() {
        return texture;
    }
    
    private void createMipmaps() {
        {
            generated = true;
            ExecutorService executor = Executors.newFixedThreadPool(THREADS_MAX);
            List<Future<?>> futures = new ArrayList<>();
            
            int steps = (int) Math.ceil(Math.log(Math.min(texture.getHeight(), texture.getWidth())) / Math.log(2));
    
            for (int m = 0; m < steps; m++) {
                if (m != 0) {
                    futures.clear();
                    data.add(new ArrayList<>());
                    BufferedImage prev = getMipmap(0, m - 1);
                    BufferedImage img = new BufferedImage(prev.getWidth(), prev.getHeight() / 2, prev.getType());
                    
                    for (int i = 0; i < img.getHeight(); i++) {
                        int finalI = i;
                        Future<?> future = executor.submit(() -> {
                            for (int j = 0; j < img.getWidth(); j++) {
                                Color c;
                                Color c1;
                                Color c2;
                                try {
                                    c1 = new Color(prev.getRGB(j, 2 * finalI));
                                    try {
                                        c2 = new Color(prev.getRGB(j, 2 * finalI + 1));
                                        c = interpolate(c1, c2, 0.5);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        c = c1;
                                    }
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    c = new Color(0);
                                }
                                img.setRGB(j, finalI, c.getRGB());
                            }
                        });
                        futures.add(future);
                    }
    
                    for (Future<?> future : futures) {
                        try {
                            future.get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                
                    data.get(m).add(img);
                    //System.out.println("Width: " + img.getWidth() + " Height: " + img.getHeight());
                }
                for (int n = 1; n < steps; n++) {
                    futures.clear();
                    BufferedImage prev = getMipmap(n - 1, m);
                    BufferedImage img = new BufferedImage(prev.getWidth() / 2, prev.getHeight(), prev.getType());
                    for (int i = 0; i < img.getHeight(); i++) {
                        int finalI = i;
                        Future<?> future = executor.submit(() -> {
                            for (int j = 0; j < img.getWidth(); j++) {
                                Color c;
                                Color c1;
                                Color c2;
                                try {
                                    c1 = new Color(prev.getRGB(2 * j, finalI));
                                    try {
                                        c2 = new Color(prev.getRGB(2 * j + 1, finalI));
                                        c = interpolate(c1, c2, 0.5);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        c = c1;
                                    }
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    c = new Color(0);
                                }
                                img.setRGB(j, finalI, c.getRGB());
                            }
                        });
                        futures.add(future);
                    }
                    for (Future<?> future : futures) {
                        try {
                            future.get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    data.get(m).add(img);
                    //System.out.println("Width: " + img.getWidth() + " Height: " + img.getHeight());
                }
            }
            executor.shutdown();
            try {
                executor.awaitTermination(120, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //System.out.println("X: " + countX() + ", Y: " + countY());
        }
    }
    
    public BufferedImage getMipmap(int x, int y) {
        if (!generated && (x != 0 || y != 0)) {
            createMipmaps();
        }
        y = Math.max(0, Math.min(data.size() - 1, y));
        x = Math.max(0, Math.min(data.get(y).size() - 1, x));
        
        return data.get(y).get(x);
    }
    
    public BufferedImage getMipmapUnclamped(int x, int y) {
        if (!generated && (x != 0 || y != 0)) {
            createMipmaps();
        }
        return data.get(y).get(x);
    }
    
    public Color getColor(double u, double v, double mmU, double mmV, Filtering filter) {
        BufferedImage mm;
        int level = (int) Math.max(Math.min(Math.round(mmU), countX() - 1), (int) Math.min(Math.round(mmV), countY() - 1));
        switch (filter) {
            case OFF:
                mm = getMipmap(level, level);
                return TextureUtils.getColor(mm, u, v, filter);
            case BILINEAR:
                mm = getMipmap(level, level);
                return TextureUtils.getColor(mm, u, v, filter);
            case TRILINEAR:
                return getColor(u, v, Math.max(mmU, mmV), Math.max(mmU, mmV));
            case ANISOTROPIC:
                return getColor(u, v, mmU, mmV);
                default:
                    return Color.BLACK;
        }
    }
    
    public Color getColor(double u, double v, double mmU, double mmV) {
        
        Color tl = TextureUtils.getColor(getMipmap((int) Math.floor(mmU), (int) Math.floor(mmV)), u, v);
        Color tr = TextureUtils.getColor(getMipmap((int) Math.min(Math.ceil(mmU), countX() - 1), (int) Math.floor(mmV)), u, v);
        Color bl = TextureUtils.getColor(getMipmap((int) Math.floor(mmU), (int) Math.min(Math.ceil(mmV), countY() - 1)), u, v);
        Color br = TextureUtils.getColor(getMipmap((int) Math.min(Math.ceil(mmU), countX() - 1), (int) Math.min(Math.ceil(mmV), countY() - 1)), u, v);
        
        double ax = 1 - Math.abs(Math.min(Math.ceil(mmU), countX() - 1) - mmU);
        double ay = 1 - Math.abs(Math.min(Math.ceil(mmV), countY() - 1) - mmV);
    
        Color t = interpolate(tl, tr, ax);
        Color b = interpolate(bl, br, ax);
    
        return interpolate(t, b, ay);
    }
    
    public int countX() {
        //return data.get(0).size();
        if (texture == null) {
            return 0;
        }
        return (int) Math.ceil(Math.log(Math.min(texture.getHeight(), texture.getWidth())) / Math.log(2));
    }
    
    public int countY() {
        //return data.size();
        if (texture == null) {
            return 0;
        }
        return (int) Math.ceil(Math.log(Math.min(texture.getHeight(), texture.getWidth())) / Math.log(2));
    }
}
