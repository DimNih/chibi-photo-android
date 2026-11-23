import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateBanner {
    public static void main(String[] args) throws Exception {
        File assetsDir = new File("assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

        generateBanner(new File(assetsDir, "banner.png"));
        generateChibiLogo(new File(assetsDir, "logo-chibi.png"), 512);
        System.out.println("Generated flat blue assets/banner.png and assets/logo-chibi.png");
    }

    private static void setupHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void generateBanner(File outFile) throws Exception {
        int w = 1280;
        int h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        // 1. Flat Solid Navy Blue Base (#0F172A) - ZERO GRADIENTS
        g.setColor(new Color(0x0F, 0x17, 0x2A));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));

        // 2. Flat Raised Blue Stage Panel on the right
        g.setColor(new Color(0x16, 0x20, 0x38));
        g.fill(new RoundRectangle2D.Float(740, 28, 512, h - 56, 16, 16));
        g.setColor(new Color(0x26, 0x35, 0x54));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(740, 28, 512, h - 56, 16, 16));

        // Subtle solid lattice lines inside right stage
        g.setColor(new Color(0x1E, 0x2D, 0x4D));
        g.setStroke(new BasicStroke(1f));
        for (int x = 776; x < 1220; x += 36) {
            g.drawLine(x, 44, x, h - 44);
        }
        for (int y = 56; y < h - 44; y += 36) {
            g.drawLine(756, y, 1236, y);
        }

        // Outer solid border
        g.setColor(new Color(0x26, 0x35, 0x54));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 24, 24));

        // 3. Draw Flat Cel-Shaded Chibi Mascot on right stage
        drawChibiMascot(g, 996f, 204f, 276f, true);

        // 4. Left side clean typography (Minimal, concise, zero clutter)
        int leftX = 64;

        // Solid Royal Blue Pill Tag
        g.setColor(new Color(0x25, 0x63, 0xEB));
        g.fill(new RoundRectangle2D.Float(leftX, 62, 196, 30, 8, 8));
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.drawString("ANDROID · 240 HZ MESH", leftX + 18, 81);

        // App Title: "Chibi Photo"
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 60));
        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.drawString("Chibi Photo", leftX, 164);

        // Short, clean one-line subtitle
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 21));
        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.drawString("Interactive 2D spring-mesh studio for chibi art & portraits", leftX, 206);

        // 3 concise flat stat chips at bottom-left
        String[][] tags = {
            {"TOOLS", "Pinch · Poke · Swirl · Tilt"},
            {"PHYSICS", "240 Hz Symplectic Spring"},
            {"EXPORT", "GIF Loop & PNG Frame"}
        };
        int cx = leftX;
        int cy = 264;
        int[] widths = {216, 208, 192};
        for (int i = 0; i < tags.length; i++) {
            int cw = widths[i];
            g.setColor(new Color(0x16, 0x20, 0x38));
            g.fill(new RoundRectangle2D.Float(cx, cy, cw, 68, 12, 12));
            g.setColor(new Color(0x26, 0x35, 0x54));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Float(cx, cy, cw, 68, 12, 12));

            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            g.setColor(new Color(0x64, 0x74, 0x8B));
            g.drawString(tags[i][0], cx + 16, cy + 25);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.setColor(i == 0 ? new Color(0x60, 0xA5, 0xFA) : new Color(0xF1, 0xF5, 0xF9));
            g.drawString(tags[i][1], cx + 16, cy + 48);

            cx += cw + 12;
        }

        g.dispose();
        ImageIO.write(img, "PNG", outFile);
    }

    private static void generateChibiLogo(File outFile, int size) throws Exception {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        // Solid Royal Blue Icon Base (#2563EB)
        g.setColor(new Color(0x25, 0x63, 0xEB));
        g.fill(new RoundRectangle2D.Float(0, 0, size, size, size * 0.22f, size * 0.22f));

        drawChibiMascot(g, size * 0.5f, size * 0.52f, size * 0.84f, false);

        g.dispose();
        ImageIO.write(img, "PNG", outFile);
    }

    private static void drawChibiMascot(Graphics2D g, float cx, float cy, float s, boolean showVectors) {
        float half = s * 0.5f;

        // Solid flat circle backdrop
        g.setColor(new Color(0x0A, 0x10, 0x1D));
        g.fill(new Ellipse2D.Float(cx - half * 0.92f, cy - half * 0.92f, s * 0.92f, s * 0.92f));
        g.setColor(new Color(0x25, 0x63, 0xEB));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Float(cx - half * 0.92f, cy - half * 0.92f, s * 0.92f, s * 0.92f));

        // Flat back hair
        g.setColor(new Color(0x47, 0x55, 0x69));
        g.fill(new Ellipse2D.Float(cx - s * 0.34f, cy - s * 0.32f, s * 0.68f, s * 0.64f));

        // Flat blue collar
        Path2D collar = new Path2D.Float();
        collar.moveTo(cx - s * 0.20f, cy + s * 0.37f);
        collar.lineTo(cx - s * 0.13f, cy + s * 0.22f);
        collar.lineTo(cx + s * 0.13f, cy + s * 0.22f);
        collar.lineTo(cx + s * 0.20f, cy + s * 0.37f);
        collar.closePath();
        g.setColor(new Color(0x1D, 0x4E, 0xD8));
        g.fill(collar);

        // Chibi Face with stretched right cheek
        float pinchOffset = showVectors ? s * 0.08f : 0f;
        Path2D face = new Path2D.Float();
        face.moveTo(cx - s * 0.26f, cy - s * 0.06f);
        face.curveTo(cx - s * 0.28f, cy - s * 0.24f, cx + s * 0.28f, cy - s * 0.24f, cx + s * 0.26f, cy - s * 0.06f);
        face.curveTo(cx + s * 0.35f + pinchOffset, cy + s * 0.14f, cx + s * 0.24f + pinchOffset * 0.5f, cy + s * 0.27f, cx, cy + s * 0.27f);
        face.curveTo(cx - s * 0.24f, cy + s * 0.27f, cx - s * 0.34f, cy + s * 0.14f, cx - s * 0.26f, cy - s * 0.06f);
        face.closePath();

        g.setColor(new Color(0xFF, 0xE9, 0xDC));
        g.fill(face);
        g.setColor(new Color(0x0A, 0x10, 0x1D));
        g.setStroke(new BasicStroke(s * 0.022f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(face);

        // Solid flat blush ovals
        g.setColor(new Color(0xFB, 0x71, 0x85));
        g.fill(new Ellipse2D.Float(cx - s * 0.26f, cy + s * 0.05f, s * 0.14f, s * 0.095f));
        g.fill(new Ellipse2D.Float(cx + s * 0.13f + pinchOffset * 0.55f, cy + s * 0.05f, s * 0.16f, s * 0.105f));

        // Big expressive anime eyes
        g.setColor(new Color(0x0A, 0x10, 0x1D));
        g.fill(new Ellipse2D.Float(cx - s * 0.17f, cy - s * 0.04f, s * 0.105f, s * 0.125f));
        g.fill(new Ellipse2D.Float(cx + s * 0.065f, cy - s * 0.04f, s * 0.105f, s * 0.125f));

        // Solid Royal Blue lower iris
        g.setColor(new Color(0x25, 0x63, 0xEB));
        g.fill(new Ellipse2D.Float(cx - s * 0.155f, cy + s * 0.005f, s * 0.075f, s * 0.072f));
        g.fill(new Ellipse2D.Float(cx + s * 0.080f, cy + s * 0.005f, s * 0.075f, s * 0.072f));

        // White eye glints
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(cx - s * 0.15f, cy - s * 0.025f, s * 0.042f, s * 0.042f));
        g.fill(new Ellipse2D.Float(cx + s * 0.085f, cy - s * 0.025f, s * 0.042f, s * 0.042f));

        // Cute W smile
        Path2D mouth = new Path2D.Float();
        mouth.moveTo(cx - s * 0.045f, cy + s * 0.11f);
        mouth.quadTo(cx - s * 0.02f, cy + s * 0.145f, cx, cy + s * 0.115f);
        mouth.quadTo(cx + s * 0.02f, cy + s * 0.145f, cx + s * 0.045f, cy + s * 0.11f);
        g.setColor(new Color(0x0A, 0x10, 0x1D));
        g.setStroke(new BasicStroke(s * 0.018f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(mouth);

        // Front bangs
        Path2D bangs = new Path2D.Float();
        bangs.moveTo(cx - s * 0.29f, cy - s * 0.03f);
        bangs.lineTo(cx - s * 0.19f, cy - s * 0.11f);
        bangs.lineTo(cx - s * 0.12f, cy - s * 0.03f);
        bangs.lineTo(cx - s * 0.01f, cy - s * 0.13f);
        bangs.lineTo(cx + s * 0.07f, cy - s * 0.03f);
        bangs.lineTo(cx + s * 0.18f, cy - s * 0.11f);
        bangs.lineTo(cx + s * 0.29f, cy - s * 0.03f);
        bangs.curveTo(cx + s * 0.30f, cy - s * 0.31f, cx - s * 0.30f, cy - s * 0.31f, cx - s * 0.29f, cy - s * 0.03f);
        bangs.closePath();

        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.fill(bangs);
        g.setColor(new Color(0x0A, 0x10, 0x1D));
        g.setStroke(new BasicStroke(s * 0.020f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(bangs);

        // Solid Royal Blue Visor
        RoundRectangle2D visor = new RoundRectangle2D.Float(cx - s * 0.22f, cy - s * 0.26f, s * 0.44f, s * 0.115f, s * 0.05f, s * 0.05f);
        g.setColor(new Color(0x25, 0x63, 0xEB));
        g.fill(visor);
        g.setColor(new Color(0x0A, 0x10, 0x1D));
        g.draw(visor);

        // Visor highlight bar
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(s * 0.015f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int)(cx - s * 0.16f), (int)(cy - s * 0.215f), (int)(cx + s * 0.02f), (int)(cy - s * 0.215f));

        if (showVectors) {
            float targetX = cx + s * 0.25f + pinchOffset * 0.7f;
            float targetY = cy + s * 0.11f;
            g.setColor(new Color(0x3B, 0x82, 0xF6));
            g.setStroke(new BasicStroke(2.2f));
            g.draw(new Ellipse2D.Float(targetX - 24, targetY - 24, 48, 48));
            g.drawLine((int)targetX, (int)targetY, (int)(targetX + 32), (int)(targetY + 10));
            g.fill(new Ellipse2D.Float(targetX + 28, targetY + 6, 8, 8));
        }
    }
}

