var fs = require('fs');

var filePaths = [
  './reports/November Patreon Report.csv',
  './reports/December Patreon Report.csv',
  './reports/January Patreon Report.csv',
  './reports/February Patreon Report.csv',
  './reports/March Patreon Report.csv',
  './reports/April Patreon Report.csv',
  './reports/May Patreon Report.csv',
  './reports/PatronReportJune.csv'
];

var files = filePaths.map(path => fs.readFileSync(path, 'utf8'));

var dataByUser = {};

files.forEach((file, i) => {
  var rows = file.split(/[\r\n]+/g).slice(2); // split by line breaks, slice to remove header rows

  rows.forEach(row => {
    if (!row) { return; } // some rows are just blank lines

    var rowData = row.split(/\,/g); // split by commas
    if (rowData.length !== 4) { console.log(JSON.stringify(rowData)); throw new Error("Invalid row data!"); }
    var [first, last, email, amount] = rowData;

    if (isNaN(Number(amount))) { console.log(JSON.stringify(rowData)); throw new Error("Invalid pledge amount!"); }

    if (!dataByUser[email]) {
      dataByUser[email] = { first, last, email, amount: 0, pledges: [] };
    }
    if (first !== dataByUser[email].first || last !== dataByUser[email].last) {
      console.log(`${email} changed their name from ${dataByUser[email].first} ${dataByUser[email].last} to ${first} ${last}`);
    }
    dataByUser[email].amount += Number(amount) * 100;
    dataByUser[email].pledges[i] = amount;
  });
});

// sorting from highest to lowest to match the other CSVs
var keysSortedByTotals = Object.keys(dataByUser).sort((a, b) => {
  return dataByUser[b].amount - dataByUser[a].amount;
});

// var CSVString = keysSortedByTotals.reduce((acc, key) => {
//   var {first, last, email, amount, pledges} = dataByUser[key];
//   acc += `${first},${last},${email},${amount/100}\n`;
//   return acc;
// }, `FirstName,LastName,Email,Pledge\n,,,\n`);

var CSVStringWithMonths = keysSortedByTotals.reduce((acc, key) => {
  var {first, last, email, amount, pledges} = dataByUser[key];
  acc += `${first},${last},${email},${amount/100},${pledges}\n`;
  return acc;
}, `FirstName,LastName,Email,Pledge,Nov,Dec,Jan,Feb,Mar,Apr,May,June\n,,,,,,,,,,,\n`);

// fs.writeFileSync("./totals.csv", CSVString);
fs.writeFileSync("./totals.js.csv", CSVStringWithMonths);

console.log("Success!");
