#!/usr/bin/env python3
"""
Script to parse and display plant information from markdown files with YAML frontmatter.
"""

import os
import re
import yaml

def extract_frontmatter(content):
    """Extract YAML frontmatter from markdown content."""
    match = re.match(r'^---\n(.*?)\n---\n', content, re.DOTALL)
    if match:
        return yaml.safe_load(match.group(1))
    return None

def parse_plant_files(directory):
    """Parse all plant markdown files in the directory."""
    plants = []
    
    for filename in os.listdir(directory):
        if filename.endswith('.md') and filename != 'README.md':
            filepath = os.path.join(directory, filename)
            with open(filepath, 'r') as f:
                content = f.read()
                metadata = extract_frontmatter(content)
                if metadata:
                    metadata['filename'] = filename
                    plants.append(metadata)
    
    return plants

def display_plants(plants):
    """Display plant information in a formatted way."""
    print(f"Found {len(plants)} plants:\n")
    
    for plant in sorted(plants, key=lambda x: x.get('name', '')):
        print(f"{'='*60}")
        print(f"Name: {plant.get('name', 'Unknown')}")
        print(f"Type: {plant.get('type', 'Unknown')}")
        print(f"Scientific Name: {plant.get('scientific_name', 'Unknown')}")
        print(f"Family: {plant.get('family', 'Unknown')}")
        print(f"Native To: {plant.get('native_to', 'Unknown')}")
        
        # Display type-specific information
        if 'blooming_season' in plant:
            print(f"Blooming Season: {plant['blooming_season']}")
        if 'colors' in plant:
            print(f"Colors: {plant['colors']}")
        if 'height' in plant:
            print(f"Height: {plant['height']}")
        if 'lifespan' in plant:
            print(f"Lifespan: {plant['lifespan']}")
        
        print(f"Sunlight: {plant.get('sunlight', 'Unknown')}")
        print(f"Water Needs: {plant.get('water_needs', 'Unknown')}")
        print(f"Hardiness Zones: {plant.get('hardiness_zones', 'Unknown')}")
        print()

if __name__ == '__main__':
    script_dir = os.path.dirname(os.path.abspath(__file__))
    plants = parse_plant_files(script_dir)
    display_plants(plants)
