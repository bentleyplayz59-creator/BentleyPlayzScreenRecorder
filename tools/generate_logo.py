from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT = Path("app/src/main/res/drawable-nodpi")
OUT.mkdir(parents=True, exist_ok=True)

SIZE = 512
image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)

# Deep navy rounded-square app tile.
draw.rounded_rectangle((18, 18, 494, 494), radius=112, fill=(14, 20, 42, 255))

# Subtle inset border.
draw.rounded_rectangle(
    (30, 30, 482, 482), radius=100,
    outline=(89, 125, 255, 190), width=5
)

# Camera/display frame.
draw.rounded_rectangle(
    (105, 112, 407, 350), radius=35,
    fill=(27, 39, 75, 255), outline=(111, 148, 255, 255), width=12
)

# Screen recording symbol.
draw.ellipse((205, 177, 307, 279), fill=(255, 75, 100, 255))
draw.ellipse((224, 196, 288, 260), fill=(255, 110, 126, 255))

# Small audio waveform.
wave_x = [142, 158, 174, 190, 206, 222, 238, 254, 270, 286, 302, 318, 334, 350, 366]
heights = [18, 30, 44, 25, 55, 76, 48, 32, 52, 72, 40, 26, 48, 34, 18]
for x, h in zip(wave_x, heights):
    draw.rounded_rectangle(
        (x - 4, 410 - h // 2, x + 4, 410 + h // 2),
        radius=5, fill=(117, 157, 255, 255)
    )

# BP monogram in the lower display area.
try:
    font = ImageFont.truetype("DejaVuSans-Bold.ttf", 38)
except OSError:
    font = ImageFont.load_default()

draw.text((256, 320), "BP", font=font, anchor="mm", fill=(235, 241, 255, 255))

image = image.filter(ImageFilter.SMOOTH)
image.save(OUT / "bentley_logo.png", optimize=True)
print(f"Generated {OUT / 'bentley_logo.png'}")
