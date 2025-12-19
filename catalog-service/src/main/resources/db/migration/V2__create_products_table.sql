CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL CHECK (base_price >= 0),
    category_id BIGINT NOT NULL,
    
    -- Product Variants as Attributes
    available_sizes TEXT[],
    available_colors TEXT[],
    stock_per_variant JSONB,
    
    -- Image URLs (validated before save)
    image_urls TEXT[] NOT NULL,
    
    -- Status & Visibility
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_featured BOOLEAN NOT NULL DEFAULT false,
    
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_category FOREIGN KEY (category_id) 
        REFERENCES categories(id),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED'))
);

CREATE INDEX idx_products_seller ON products(seller_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_products_featured ON products(is_featured) WHERE is_featured = true;
