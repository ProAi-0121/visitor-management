#!/usr/bin/env python3

import os
from PIL import Image, ImageDraw

# Define the mipmap directories and their corresponding sizes
MIPMAP_SIZES = {
    'mipmap-mdpi': 48,    # 1x (baseline)
    'mipmap-hdpi': 72,    # 1.5x
    'mipmap-xhdpi': 96,   # 2x
    'mipmap-xxhdpi': 144, # 3x
    'mipmap-xxxhdpi': 192 # 4x
}

def create_directory(path):
    """Create directory if it doesn't exist"""
    if not os.path.exists(path):
        os.makedirs(path)
        print(f"Created directory: {path}")

def create_circular_mask(size):
    """Create a circular mask for the icon"""
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    return mask

def create_round_icon(image, size):
    """Create a round version of the icon"""
    # Create a new image with transparency
    output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Create circular mask
    mask = create_circular_mask(size)
    
    # Paste the resized image using the circular mask
    output.paste(image, (0, 0), mask)
    return output

def convert_logo(logo_path, output_base_path):
    """Convert logo to different resolutions in WebP format, including round versions"""
    try:
        # Open the original logo
        with Image.open(logo_path) as img:
            # Convert to RGBA if not already
            if img.mode != 'RGBA':
                img = img.convert('RGBA')
            
            # Process for each mipmap directory
            for mipmap_dir, size in MIPMAP_SIZES.items():
                # Create the output directory
                output_dir = os.path.join(output_base_path, mipmap_dir)
                create_directory(output_dir)
                
                # Resize the image
                resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
                
                # Save regular version
                output_path = os.path.join(output_dir, 'ic_launcher.webp')
                resized_img.save(
                    output_path, 
                    'WEBP',
                    quality=100,
                    method=6,  # Highest compression method
                    lossless=True  # Use lossless compression for logo
                )
                print(f"Created regular icon: {output_path} ({size}x{size})")
                
                # Create and save round version
                round_img = create_round_icon(resized_img, size)
                round_output_path = os.path.join(output_dir, 'ic_launcher_round.webp')
                round_img.save(
                    round_output_path,
                    'WEBP',
                    quality=100,
                    method=6,
                    lossless=True
                )
                print(f"Created round icon: {round_output_path} ({size}x{size})")
                
    except Exception as e:
        print(f"Error processing logo: {e}")
        return False
    
    return True

def main():
    # Get the script's directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Define paths
    logo_path = os.path.join(script_dir, '..', 'app', 'src', 'main', 'res', 'drawable', 'logo.png')
    output_base_path = os.path.join(script_dir, '..', 'app', 'src', 'main', 'res')
    
    # Check if logo exists
    if not os.path.exists(logo_path):
        print(f"Error: Logo not found at {logo_path}")
        return
    
    print("Starting logo conversion...")
    if convert_logo(logo_path, output_base_path):
        print("\nLogo conversion completed successfully!")
        print("\nResolutions created (both regular and round):")
        for mipmap_dir, size in MIPMAP_SIZES.items():
            print(f"- {mipmap_dir}: {size}x{size}px")
    else:
        print("\nLogo conversion failed!")

if __name__ == "__main__":
    main() 