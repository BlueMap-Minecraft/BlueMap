import { execSync } from "node:child_process";
import { readdirSync } from "node:fs";
import path from "node:path";

// doesn't really matter, just setting something consistant and close enough for us europeans
process.env.TZ = "Europe/Berlin";

function parse(str) {
    const blame = execSync(`git blame --porcelain ${str}`).toString("utf8").trim().split("\n");
    const commitMap = new Map();
    const nodes = [];
    const path = [];
    let inMultiLineString = false;
    let multiLineStringLastUpdated = null;
    // let multiLineStringValue = "";
    for (let i = 0; i < blame.length; i++) {
        const hash = blame[i].split(" ")[0];
        i++;
        if (!commitMap.has(hash)) {
            const commit = {};
            let j = 0;
            while (true) {
                const line = blame[i + j];
                if (line[0] === "\t") break;
                const [key, ...rest] = line.split(" ");
                const val = rest.join(" ");
                commit[key.replace(/-\S/g, (s) => s.slice(1).toUpperCase())] = val;
                j++;
            }
            commitMap.set(hash, commit);
            i += j;
        }
        const commit = commitMap.get(hash);
        let lastUpdated = parseInt(commit.authorTime);
        if (inMultiLineString) {
            const line = blame[i].slice(1).trimEnd();
            if (line.endsWith('"""')) {
                // multiLineStringValue += "\n" + line.slice(0, -3);
                nodes.push({
                    path: [...path],
                    lastUpdated: multiLineStringLastUpdated,
                    // value: multiLineStringValue,
                });
                inMultiLineString = false;
                multiLineStringLastUpdated = null;
                // multiLineStringValue = "";
                path.pop();
                continue;
            } else {
                if (lastUpdated > multiLineStringLastUpdated)
                    multiLineStringLastUpdated = commit.authorTime;
                // multiLineStringValue += "\n" + blame[i].slice(1);
                continue;
            }
        }
        const line = blame[i].slice(1).trim();
        if (line === "{") continue;
        if (line === "}") {
            path.pop();
            continue;
        }
        if (!line.includes('"')) {
            path.push(line.split(":")[0].split(" ")[0]);
            continue;
        }
        const [key, rest] = line.split(":");
        if (rest.trimStart().startsWith('"""')) {
            inMultiLineString = true;
            multiLineStringLastUpdated = lastUpdated;
            // multiLineStringValue = rest.trimStart().slice(3);
            path.push(key);
            continue;
        }
        nodes.push({
            path: [...path, key],
            lastUpdated,
            // value: rest.trimStart().slice(1, -1)
        });
    }
    return nodes;
}

const langFolder = "../../common/webapp/public/lang/";
const languageFiles = readdirSync(langFolder).filter(
    (f) => f.endsWith(".conf") && f !== "settings.conf"
);

const languages = languageFiles.map((file) => {
    const nodes = parse(path.join(langFolder, file));
    const name = file.split(".").reverse().slice(1).reverse().join(".");
    return {
        name,
        nodes,
    };
});

const sourceLanguageName = "en";
const sourceLanguage = languages.find((l) => l.name === sourceLanguageName);
if (!sourceLanguage) throw new Error(`Source language "${sourceLanguageName}" not found!`);
languages.splice(languages.indexOf(sourceLanguage), 1);

function diff(source, other) {
    const sourceKeys = source.map((n) => n.path.join("."));
    const otherKeys = other.map((n) => n.path.join("."));
    const missing = sourceKeys.filter((sk) => !otherKeys.includes(sk));
    const extra = otherKeys.filter((ok) => !sourceKeys.includes(ok));
    const outdated = other
        .map((n) => {
            const sourceNode = source.find((sn) => sn.path.join(".") === n.path.join("."));
            return { ...n, sourceNode };
        })
        .filter((n) => {
            return n.sourceNode && n.sourceNode.lastUpdated > n.lastUpdated;
        });
    return {
        missing,
        extra,
        outdated,
    };
}

const upToDate = [];
for (const { name, nodes } of languages) {
    const { missing, extra, outdated } = diff(sourceLanguage.nodes, nodes);

    if (missing.length + extra.length + outdated.length === 0) {
        upToDate.push(name);
        continue;
    }

    console.log(`=== ${name} ===`);
    if (missing.length) {
        console.log(`Missing (${missing.length}):`);
        for (const key of missing) console.log("-", key);
        console.log();
    }
    if (extra.length) {
        console.log(`Extra (${extra.length}):`);
        for (const key of extra) console.log("-", key);
        console.log();
    }
    if (outdated.length) {
        console.log(`Outdated (${outdated.length}):`);
        for (const { path, lastUpdated, sourceNode } of outdated)
            console.log(
                "-",
                path.join("."),
                `(updated ${new Date(lastUpdated * 1000).toLocaleString(
                    "de"
                )}, source updated ${new Date(sourceNode.lastUpdated * 1000).toLocaleString("de")})`
            );
        console.log();
    }
}

if (upToDate.length) console.log("Up to date:", upToDate.join(", "));
