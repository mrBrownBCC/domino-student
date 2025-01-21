const fs = require("fs");
const path = require("path");

// Path to the CSS file
const cssFilePath = path.join(__dirname, "html/build/dist/styles.css");

// CSS rule to append
const cssRule = `
/* Ensure canvas dimensions override inline styles */
canvas {
    width: auto !important;
    height: auto !important;
    max-width: 100% !important;
    max-height: 100% !important;
    aspect-ratio : 3/2 !important;
    display: block;
    margin: 0 auto;
    border: 1px solid #000; /* Optional border for debugging */
}
`;

// Check if the file exists
if (fs.existsSync(cssFilePath)) {
    // Append the rule to the CSS file
    fs.appendFileSync(cssFilePath, cssRule, "utf8");
    console.log("CSS rule appended to styles.css");
} else {
    console.error("styles.css not found. Ensure the build process has run.");
}
