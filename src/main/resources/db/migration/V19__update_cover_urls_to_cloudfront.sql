-- Migra URLs de capa do S3 direto para CloudFront
-- O bucket vidalongaflix-media passou a ser privado (Block Public Access + OAC) em 30/05/2026.
-- Acesso direto ao S3 retorna 403 — apenas o CloudFront d27rhgr1cr7axs pode servir os arquivos.

UPDATE videos
SET cover = REPLACE(cover,
    'https://vidalongaflix-media.s3.us-east-2.amazonaws.com/',
    'https://d27rhgr1cr7axs.cloudfront.net/')
WHERE cover LIKE 'https://vidalongaflix-media.s3.us-east-2.amazonaws.com/%';

UPDATE menus
SET cover = REPLACE(cover,
    'https://vidalongaflix-media.s3.us-east-2.amazonaws.com/',
    'https://d27rhgr1cr7axs.cloudfront.net/')
WHERE cover LIKE 'https://vidalongaflix-media.s3.us-east-2.amazonaws.com/%';