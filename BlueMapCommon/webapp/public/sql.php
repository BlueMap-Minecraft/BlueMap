<?php

// !!! SET YOUR SQL-CONNECTION SETTINGS HERE: !!!

$driver   = 'mysql'; // 'mysql' (MySQL) or 'pgsql' (PostgreSQL)
$hostname = '127.0.0.1';
$port     = 3306;
$username = 'root';
$password = '';
$database = 'bluemap';

// set this to "none" if you disabled compression on your maps
$hiresCompression = 'gzip';

// !!! END - DONT CHANGE ANYTHING AFTER THIS LINE !!!





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
        $sql = new PDO("$driver:host=$hostname;dbname=$database", $username, $password);
        $sql->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    } catch (PDOException $e ) { error(500, "Failed to connect to database"); }


    // provide map-tiles
    if (startsWith($mapPath, "tiles/")) {

        // parse tile-coordinates
        preg_match_all("/tiles\/([\d\/]+)\/x(-?[\d\/]+)z(-?[\d\/]+).*/", $mapPath, $matches);
        $lod = intval($matches[1][0]);
        $tileX = intval(str_replace("/", "", $matches[2][0]));
        $tileZ = intval(str_replace("/", "", $matches[3][0]));
        $compression = $lod === 0 ? $hiresCompression : "none";

        // query for tile
        try {
            $statement = $sql->prepare("
                SELECT t.data
                FROM bluemap_map_tile t
                INNER JOIN bluemap_map m
                ON t.map = m.id
                INNER JOIN bluemap_map_tile_compression c
                ON t.compression = c.id
                WHERE m.map_id = :map_id
                AND t.lod = :lod
                AND t.x = :x
                AND t.z = :z
                AND c.compression = :compression
            ");
            $statement->bindParam( ':map_id', $mapId, PDO::PARAM_STR );
            $statement->bindParam( ':lod', $lod, PDO::PARAM_INT );
            $statement->bindParam( ':x', $tileX, PDO::PARAM_INT );
            $statement->bindParam( ':z', $tileZ, PDO::PARAM_INT );
            $statement->bindParam( ':compression', $compression, PDO::PARAM_STR);
            $statement->setFetchMode(PDO::FETCH_ASSOC);
            $statement->execute();

            // return result
            if ($line = $statement->fetch()) {
                if ($compression !== "none")
                    header("Content-Encoding: $compression");
                if ($lod === 0) {
                    header("Content-Type: application/json");
                } else {
                    header("Content-Type: image/png");
                }
                send($line["data"]);
                exit;
            }

        } catch (PDOException $e) { error(500, "Failed to fetch data"); }

        // empty json response if nothing found
        header("Content-Type: application/json");
        echo "{}";
        exit;
    }

    // provide meta-files
    try {
        $statement = $sql->prepare("
            SELECT t.value
            FROM bluemap_map_meta t
            INNER JOIN bluemap_map m
            ON t.map = m.id
            WHERE m.map_id = :map_id
            AND t.key = :map_path
        ");
        $statement->bindParam( ':map_id', $mapId, PDO::PARAM_STR );
        $statement->bindParam( ':map_path', $mapPath, PDO::PARAM_STR );
        $statement->setFetchMode(PDO::FETCH_ASSOC);
        $statement->execute();

        if ($line = $statement->fetch()) {
            header("Content-Type: ".getMimeType($mapPath));
            send($line["value"]);
            exit;
        }
    } catch (PDOException $e) { error(500, "Failed to fetch data"); }

}

// no match => 404
error(404);