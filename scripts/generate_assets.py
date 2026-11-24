import os
from PIL import Image, ImageDraw, ImageFont

def draw_chibi_logo(size=512):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = float(size)

    # 1. Light Blue circular frame (flat pure tones, zero gradient)
    bg_pad = s * 0.04
    draw.ellipse([bg_pad, bg_pad, s - bg_pad, s - bg_pad], fill=(186, 230, 253, 255), outline=(2, 132, 199, 255), width=int(s * 0.018))
    
    inner_pad = s * 0.08
    draw.ellipse([inner_pad, inner_pad, s - inner_pad, s - inner_pad], fill=(224, 242, 254, 255))

    # Center is (s*0.5, s*0.52)
    cx = s * 0.5
    cy = s * 0.53

    outline_col = (15, 23, 42, 255) # Slate 900
    hair_col = (2, 132, 199, 255)    # Sky 600 flat blue
    skin_col = (255, 241, 230, 255)  # Soft peach/ivory
    pink_ear = (251, 113, 133, 255)  # Rose pink
    blush_col = (251, 113, 133, 220)
    eye_col = (15, 23, 42, 255)
    eye_blue = (56, 189, 248, 255)   # Sky 400
    white = (255, 255, 255, 255)

    line_w = max(3, int(s * 0.022))

    # Cat / Chibi Ears
    # Left Ear
    draw.polygon([(s * 0.30, cy - s * 0.16), (s * 0.22, cy - s * 0.36), (s * 0.42, cy - s * 0.26)], fill=hair_col, outline=outline_col)
    draw.polygon([(s * 0.30, cy - s * 0.18), (s * 0.25, cy - s * 0.33), (s * 0.39, cy - s * 0.26)], fill=pink_ear)
    # Re-outline left ear
    draw.line([(s * 0.30, cy - s * 0.16), (s * 0.22, cy - s * 0.36), (s * 0.42, cy - s * 0.26), (s * 0.30, cy - s * 0.16)], fill=outline_col, width=line_w)

    # Right Ear
    draw.polygon([(s * 0.70, cy - s * 0.16), (s * 0.78, cy - s * 0.36), (s * 0.58, cy - s * 0.26)], fill=hair_col, outline=outline_col)
    draw.polygon([(s * 0.70, cy - s * 0.18), (s * 0.75, cy - s * 0.33), (s * 0.61, cy - s * 0.26)], fill=pink_ear)
    # Re-outline right ear
    draw.line([(s * 0.70, cy - s * 0.16), (s * 0.78, cy - s * 0.36), (s * 0.58, cy - s * 0.26), (s * 0.70, cy - s * 0.16)], fill=outline_col, width=line_w)

    # Head Hair Base
    head_r = s * 0.27
    draw.ellipse([cx - head_r, cy - head_r - s * 0.03, cx + head_r, cy + head_r - s * 0.03], fill=hair_col, outline=outline_col, width=line_w)

    # Chubby Squishy Face Skin
    face_w = s * 0.24
    face_h = s * 0.21
    draw.ellipse([cx - face_w, cy - face_h + s * 0.03, cx + face_w, cy + face_h + s * 0.03], fill=skin_col, outline=outline_col, width=line_w)

    # Hair Bangs
    bangs = [
        (cx - s * 0.23, cy - s * 0.06),
        (cx - s * 0.16, cy + s * 0.02),
        (cx - s * 0.10, cy - s * 0.05),
        (cx - s * 0.03, cy + s * 0.03),
        (cx + s * 0.04, cy - s * 0.05),
        (cx + s * 0.12, cy + s * 0.02),
        (cx + s * 0.18, cy - s * 0.04),
        (cx + s * 0.23, cy - s * 0.06),
        (cx + s * 0.21, cy - s * 0.22),
        (cx - s * 0.21, cy - s * 0.22)
    ]
    draw.polygon(bangs, fill=hair_col)
    draw.line(bangs + [bangs[0]], fill=outline_col, width=line_w)

    # Squishy Cheek Blush
    blush_r_x = s * 0.050
    blush_r_y = s * 0.035
    # Left cheek blush
    draw.ellipse([cx - s * 0.19 - blush_r_x, cy + s * 0.10 - blush_r_y, cx - s * 0.19 + blush_r_x, cy + s * 0.10 + blush_r_y], fill=blush_col)
    # Right cheek blush
    draw.ellipse([cx + s * 0.19 - blush_r_x, cy + s * 0.10 - blush_r_y, cx + s * 0.19 + blush_r_x, cy + s * 0.10 + blush_r_y], fill=blush_col)

    # Big Sparkling Anime Eyes
    eye_w = s * 0.048
    eye_h = s * 0.068
    lx = cx - s * 0.12
    rx = cx + s * 0.12
    ey = cy + s * 0.04

    for ex in [lx, rx]:
        # Dark iris
        draw.ellipse([ex - eye_w, ey - eye_h, ex + eye_w, ey + eye_h], fill=eye_col)
        # Bottom cyan glow
        draw.chord([ex - eye_w * 0.85, ey, ex + eye_w * 0.85, ey + eye_h * 0.9], start=0, end=180, fill=eye_blue)
        # Big sparkle catchlight
        sp_r = s * 0.020
        draw.ellipse([ex - eye_w * 0.45 - sp_r, ey - eye_h * 0.4 - sp_r, ex - eye_w * 0.45 + sp_r, ey - eye_h * 0.4 + sp_r], fill=white)
        # Small sparkle catchlight
        sp_small = s * 0.009
        draw.ellipse([ex + eye_w * 0.25 - sp_small, ey + eye_h * 0.3 - sp_small, ex + eye_w * 0.25 + sp_small, ey + eye_h * 0.3 + sp_small], fill=white)
        # Eyelashes
        if ex == lx:
            draw.arc([ex - eye_w * 1.2, ey - eye_h * 1.2, ex + eye_w * 1.1, ey - eye_h * 0.4], start=200, end=350, fill=outline_col, width=int(line_w * 1.3))
        else:
            draw.arc([ex - eye_w * 1.1, ey - eye_h * 1.2, ex + eye_w * 1.2, ey - eye_h * 0.4], start=190, end=340, fill=outline_col, width=int(line_w * 1.3))

    # Cute Smiling Mouth
    mouth_w = s * 0.05
    my = cy + s * 0.13
    draw.arc([cx - mouth_w, my - s * 0.02, cx + mouth_w, my + s * 0.035], start=0, end=180, fill=outline_col, width=int(line_w * 1.1))
    draw.chord([cx - mouth_w * 0.7, my, cx + mouth_w * 0.7, my + s * 0.03], start=0, end=180, fill=pink_ear)

    # Cute Little Paws squeezing cheeks
    paw_r = s * 0.045
    # Left paw
    draw.ellipse([cx - s * 0.25 - paw_r * 0.6, cy + s * 0.12 - paw_r * 0.6, cx - s * 0.25 + paw_r * 0.8, cy + s * 0.12 + paw_r * 0.8], fill=white, outline=outline_col, width=line_w)
    # Right paw
    draw.ellipse([cx + s * 0.25 - paw_r * 0.8, cy + s * 0.12 - paw_r * 0.6, cx + s * 0.25 + paw_r * 0.6, cy + s * 0.12 + paw_r * 0.8], fill=white, outline=outline_col, width=line_w)

    return img

def draw_banner():
    bw = 1200
    bh = 500
    banner = Image.new("RGBA", (bw, bh), (240, 247, 255, 255)) # Light sky blue
    draw = ImageDraw.Draw(banner)

    # Hairline border around banner
    draw.rectangle([0, 0, bw - 1, bh - 1], outline=(191, 219, 254, 255), width=3)

    # Inner white card frame
    draw.rounded_rectangle([20, 20, bw - 20, bh - 20], radius=16, fill=(255, 255, 255, 255), outline=(224, 242, 254, 255), width=2)

    # Chibi Mascot Logo on Left
    chibi = draw_chibi_logo(size=380)
    banner.paste(chibi, (50, 60), chibi)

    # Right side typography & feature badges
    tx = 470
    try:
        font_title = ImageFont.truetype("arialbd.ttf", 52)
        font_sub = ImageFont.truetype("arial.ttf", 22)
        font_badge = ImageFont.truetype("arialbd.ttf", 16)
        font_badge_desc = ImageFont.truetype("arial.ttf", 14)
    except:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_badge = ImageFont.load_default()
        font_badge_desc = ImageFont.load_default()

    # Title: Chibi Photo
    draw.text((tx, 75), "Chibi Photo", fill=(15, 23, 42, 255), font=font_title)
    draw.text((tx, 140), "Studio Foto Chibi Kenyal & Elastis (Interactive Physics)", fill=(51, 65, 85, 255), font=font_sub)

    # Feature tags in solid flat light-blue badges (no gradients)
    badges = [
        ("TEMA BIRU MUDA", "Pure Flat Palette · Zero Gradients", 195),
        ("MODE GELAP / TERANG", "Dynamic Light & Dark Theme Toggle", 255),
        ("BAHASA ID & ENG", "Full Localization Support (ID / EN)", 315),
        ("LOGO CHIBI KARTUN", "Adorable Chibi Mascot & Squishy Face", 375),
    ]

    for label, desc, by in badges:
        draw.rounded_rectangle([tx, by, tx + 180, by + 34], radius=6, fill=(224, 242, 254, 255), outline=(186, 230, 253, 255), width=1)
        draw.text((tx + 12, by + 8), label, fill=(2, 132, 199, 255), font=font_badge)
        draw.text((tx + 200, by + 9), desc, fill=(100, 116, 139, 255), font=font_badge_desc)

    return banner

if __name__ == "__main__":
    os.makedirs("assets", exist_ok=True)
    logo = draw_chibi_logo(512)
    logo.save("assets/logo-chibi.png", "PNG")
    print("Saved assets/logo-chibi.png")

    banner = draw_banner()
    banner.save("assets/banner.png", "PNG")
    print("Saved assets/banner.png")

