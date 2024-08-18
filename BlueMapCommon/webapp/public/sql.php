<?php

// !!! SET YOUR SQL-CONNECTION SETTINGS HERE: !!!

$driver   = 'mysql'; // 'mysql' (MySQL) or 'pgsql' (PostgreSQL)
$hostname = '127.0.0.1';
$port     = 3306;
$username = 'root';
$password = '';
$database = 'bluemap';

// !!! END - DONT CHANGE ANYTHING AFTER THIS LINE !!!




// compression
$compressionHeaderMap = [
    "bluemap:none" => null,
    "bluemap:gzip" => "gzip",
    "bluemap:deflate" => "deflate",
    "bluemap:zstd" => "zstd",
    "bluemap:lz4" => "lz4"
];

// meta files
$metaFileKeys = [
    "settings.json" => "bluemap:settings",
    "textures.json" => "bluemap:textures",
    "live/markers.json" => "bluemap:markers",
    "live/players.json" => "bluemap:players",
];

// mime-types for meta-files
$mimeDefault = "application/octet-stream";
$mimeTypes = [
    "txt" => "text/plain",
    "css" => "text/css",
    "csv" => "text/csv",
    "htm" => "text/html",
    "html" => "text/html",
    "js" => "text/javascript",
    "xml" => "text/xml",

    "png" => "image/png",
    "jpg" => "image/jpeg",
    "jpeg" => "image/jpeg",
    "gif" => "image/gif",
    "webp" => "image/webp",
    "tif" => "image/tiff",
    "tiff" => "image/tiff",
    "svg" => "image/svg+xml",

    "json" => "application/json",

    "mp3" => "audio/mpeg",
    "oga" => "audio/ogg",
    "wav" => "audio/wav",
    "weba" => "audio/webm",

    "mp4" => "video/mp4",
    "mpeg" => "video/mpeg",
    "webm" => "video/webm",

    "ttf" => "font/ttf",
    "woff" => "font/woff",
    "woff2" => "font/woff2"
];

// some helper functions
function error($code, $message = null) {
    global $path;

    http_response_code($code);
    header("Content-Type: text/plain");
    echo "BlueMap php-script - $code\n";
    if ($message != null) echo $message."\n";
    echo "Requested Path: $path";
    exit;
}

function startsWith($haystack, $needle) {
    return substr($haystack, 0, strlen($needle)) === $needle;
}

function issetOrElse(& $var, $fallback) {
    return isset($var) ? $var : $fallback;
}

function compressionHeader($compressionKey) {
    global $compressionHeaderMap;

    $compressionHeader = issetOrElse($compressionHeaderMap[$compressionKey], null);
    if ($compressionHeader)
        header("Content-Encoding: ".$compressionHeader);
}

function getMimeType($path) {
    global $mimeDefault, $mimeTypes;

    $i = strrpos($path, ".");
    if ($i === false) return $mimeDefault;

    $s = strrpos($path, "/");
    if ($s !== false && $i < $s) return $mimeDefault;

    $suffix = substr($path, $i + 1);
    if (isset($mimeTypes[$suffix]))
        return $mimeTypes[$suffix];

    return $mimeDefault;
}

function send($data) {
    if (is_resource($data)) {
        fpassthru($data);
    } else {
        echo $data;
    }
}

// determine relative request-path
$root = dirname($_SERVER['PHP_SELF']);
if ($root === "/" || $root === "\\") $root = "";
$uriPath = $_SERVER['REQUEST_URI'];
$path = substr($uriPath, strlen($root));

// add /
if ($path === "") {
    header("Location: $uriPath/");
    exit;
}

// root => index.html
if ($path === "/") {
    header("Content-Type: text/html");
    echo file_get_contents("index.html");
    exit;
}

if (startsWith($path, "/maps/")) {

    // determine map-path
    $pathParts = explode("/", substr($path, strlen("/maps/")), 2);
    $mapId = $pathParts[0];
    $mapPath = explode("?", $pathParts[1], 2)[0];

    // Initialize PDO
    try {
        $sql = new PDO("$driver:host=$hostname;port=$port;dbname=$database", $username, $password);
        $sql->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    } catch (PDOException $e ) { 
        error_log($e->getMessage(), 0); // Logs the detailed error message
        error(500, "Failed to connect to database");
    }

    // provide map-tiles
    if (startsWith($mapPath, "tiles/")) {

        // parse tile-coordinates
        preg_match_all("/tiles\/([\d\/]+)\/x(-?[\d\/]+)z(-?[\d\/]+).*/", $mapPath, $matches);
        $lod = intval($matches[1][0]);
        $storage = $lod === 0 ? "bluemap:hires" : "bluemap:lowres/".$lod;
        $tileX = intval(str_replace("/", "", $matches[2][0]));
        $tileZ = intval(str_replace("/", "", $matches[3][0]));

        // query for tile
        try {
            $statement = $sql->prepare("
                SELECT d.data, c.key
                FROM bluemap_grid_storage_data d
                INNER JOIN bluemap_map m
                 ON d.map = m.id
                INNER JOIN bluemap_grid_storage s
                 ON d.storage = s.id
                INNER JOIN bluemap_compression c
                 ON d.compression = c.id
                WHERE m.map_id = :map_id
                 AND s.key = :storage
                 AND d.x = :x
                 AND d.z = :z
            ");
            $statement->bindParam( ':map_id', $mapId, PDO::PARAM_STR );
            $statement->bindParam( ':storage', $storage, PDO::PARAM_STR );
            $statement->bindParam( ':x', $tileX, PDO::PARAM_INT );
            $statement->bindParam( ':z', $tileZ, PDO::PARAM_INT );
            $statement->setFetchMode(PDO::FETCH_ASSOC);
            $statement->execute();

            // return result
            if ($line = $statement->fetch()) {
                header("Cache-Control: public,max-age=86400");
                compressionHeader($line["key"]);

                if ($lod === 0) {
                    header("Content-Type: application/octet-stream");
                } else {
                    header("Content-Type: image/png");
                }

                send($line["data"]);
                exit;
            }

        } catch (PDOException $e) { 
            error_log($e->getMessage(), 0);
            error(500, "Failed to fetch data");
        }

        // no content if nothing found
        http_response_code(204);
        exit;
    }

    // provide meta-files
    $storage = issetOrElse($metaFileKeys[$mapPath], null);
    if ($storage === null && startsWith($mapPath, "assets/"))
        $storage = "bluemap:asset/".substr($mapPath, strlen("assets/"));

    if ($storage !== null) {
        try {
            $statement = $sql->prepare("
                SELECT d.data, c.key
                FROM bluemap_item_storage_data d
                INNER JOIN bluemap_map m
                 ON d.map = m.id
                INNER JOIN bluemap_item_storage s
                 ON d.storage = s.id
                INNER JOIN bluemap_compression c
                 ON d.compression = c.id
                WHERE m.map_id = :map_id
                 AND s.key = :storage
            ");
            $statement->bindParam( ':map_id', $mapId, PDO::PARAM_STR );
            $statement->bindParam( ':storage', $storage, PDO::PARAM_STR );
            $statement->setFetchMode(PDO::FETCH_ASSOC);
            $statement->execute();

            if ($line = $statement->fetch()) {
                header("Cache-Control: public,max-age=86400");
                header("Content-Type: ".getMimeType($mapPath));
                compressionHeader($line["key"]);

                send($line["data"]);
                exit;
            }
        } catch (PDOException $e) { 
            error_log($e->getMessage(), 0);
            error(500, "Failed to fetch data");
        }
    }

}

// no match => 404
error(404);
