-- PostgreSQL simple 字典不会切分连续中文。平台使用不可变的汉字 unigram/bigram 与
-- ASCII 单词混合分词，确保无需依赖数据库宿主机插件也能建立可迁移的 GIN 全文索引。
CREATE OR REPLACE FUNCTION rdp_search_tokens(input_text TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
PARALLEL SAFE
AS $$
DECLARE
    normalized TEXT := lower(coalesce(input_text,''));
    tokens TEXT;
    current_char TEXT;
    next_char TEXT;
    position INTEGER := 1;
BEGIN
    tokens := regexp_replace(normalized,'[^a-z0-9]+',' ','g');
    WHILE position <= char_length(normalized) LOOP
        current_char := substring(normalized FROM position FOR 1);
        IF current_char ~ '[一-龥]' THEN
            tokens := tokens || ' ' || current_char;
            next_char := substring(normalized FROM position + 1 FOR 1);
            IF next_char ~ '[一-龥]' THEN
                tokens := tokens || ' ' || current_char || next_char;
            END IF;
        END IF;
        position := position + 1;
    END LOOP;
    RETURN trim(tokens);
END;
$$;

ALTER TABLE data_dataset ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS
    (to_tsvector('simple',rdp_search_tokens(coalesce(name,'')||' '||coalesce(description,'')||' '||
                                             coalesce(tags,'')||' '||coalesce(category,'')||' '||field_definition::text))) STORED;
ALTER TABLE trace_entity ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS
    (to_tsvector('simple',rdp_search_tokens(entity_code||' '||entity_name||' '||entity_type||' '||
                                             properties::text||' '||coalesce(source_system,'')||' '||coalesce(source_record_id,'')))) STORED;
ALTER TABLE device ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS
    (to_tsvector('simple',rdp_search_tokens(device_code||' '||device_name||' '||device_type||' '||
                                             coalesce(model,'')||' '||coalesce(protocol,'')||' '||status))) STORED;
ALTER TABLE tpl_template ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS
    (to_tsvector('simple',rdp_search_tokens(name||' '||coalesce(tag,'')||' '||coalesce(description,'')||' '||
                                             coalesce(source,'')||' '||type||' '||content::text||' '||coalesce(schema_definition,'{}'::jsonb)::text))) STORED;
ALTER TABLE dataset_record_workflow ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS
    (to_tsvector('simple',rdp_search_tokens(search_data::text))) STORED;

CREATE INDEX idx_dataset_search_vector_v23 ON data_dataset USING GIN(search_vector);
CREATE INDEX idx_trace_search_vector_v23 ON trace_entity USING GIN(search_vector);
CREATE INDEX idx_device_search_vector_v23 ON device USING GIN(search_vector);
CREATE INDEX idx_template_search_vector_v23 ON tpl_template USING GIN(search_vector);
CREATE INDEX idx_record_search_vector_v23 ON dataset_record_workflow USING GIN(search_vector);

COMMENT ON FUNCTION rdp_search_tokens(TEXT) IS '研发生产数据中文全文检索分词：ASCII单词 + 常用汉字unigram/bigram';

CREATE OR REPLACE FUNCTION rdp_headline_text(input_text TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
PARALLEL SAFE
AS $$
DECLARE
    output TEXT := '';
    current_char TEXT;
    position INTEGER := 1;
BEGIN
    WHILE position <= char_length(coalesce(input_text,'')) LOOP
        current_char := substring(input_text FROM position FOR 1);
        output := output || current_char || CASE WHEN current_char ~ '[一-龥]' THEN ' ' ELSE '' END;
        position := position + 1;
    END LOOP;
    RETURN output;
END;
$$;

COMMENT ON FUNCTION rdp_headline_text(TEXT) IS '为PostgreSQL ts_headline提供中文字符边界；API返回前会压缩展示空格';
