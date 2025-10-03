# Plant Information Collection

This directory contains a collection of markdown files documenting different types of plants. Each file includes YAML frontmatter with structured information about the plant.

## Contents

- **rose.md** - Information about roses (flowers)
- **oak.md** - Information about oak trees
- **fern.md** - Information about ferns
- **sunflower.md** - Information about sunflowers
- **maple.md** - Information about maple trees
- **lavender.md** - Information about lavender (herb)

## YAML Frontmatter Structure

Each plant file includes YAML frontmatter with the following fields:

- `name`: Common name of the plant
- `type`: Plant category (Tree, Flower, Herb, Plant)
- `scientific_name`: Scientific/botanical name
- `family`: Taxonomic family
- `native_to`: Geographic origin
- Additional fields specific to the plant type (blooming_season, height, colors, sunlight, water_needs, hardiness_zones, etc.)

## Example

```markdown
---
name: Rose
type: Flower
scientific_name: Rosa
family: Rosaceae
native_to: Asia, Europe, North America
blooming_season: Spring, Summer
colors: Red, Pink, White, Yellow, Orange
sunlight: Full sun
water_needs: Moderate
hardiness_zones: 3-11
---

# Rose

[Plant description and details...]
```

## Usage

These files can be used to:
- Parse plant information programmatically
- Generate plant databases
- Create botanical reference materials
- Practice working with YAML frontmatter in markdown files
