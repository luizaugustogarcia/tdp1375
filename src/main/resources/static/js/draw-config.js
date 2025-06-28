var COLOR_POOL = [
  "blue", "tomato", "brown", "blueviolet", "burlywood", "cadetblue", "chartreuse", "chocolate", "coral",
  "cornflowerblue", "crimson", "cyan", "darkblue", "darkcyan", "darkgoldenrod", "darkgray",
  "darkgreen", "darkkhaki", "darkmagenta", "darkolivegreen", "darkorange", "darkorchid",
  "darkred", "darksalmon", "darkseagreen", "darkslateblue", "darkslategray", "darkslategrey",
  "darkturquoise", "darkviolet", "deeppink", "deepskyblue", "dimgray", "dimgrey", "dodgerblue",
  "firebrick", "forestgreen", "fuchsia", "goldenrod", "gold", "gray", "green",
  "greenyellow", "grey", "hotpink", "indianred", "indigo", "khaki", "lawngreen",
  "lightblue", "lightcoral", "lightgreen", "lightpink", "lightsalmon", "lightseagreen",
  "lightskyblue", "lightslategray", "lightslategrey", "lightsteelblue", "lime",
  "limegreen", "magenta", "maroon", "mediumaquamarine", "mediumblue", "mediumorchid",
  "mediumpurple", "mediumseagreen", "mediumslateblue", "mediumspringgreen", "mediumturquoise",
  "mediumvioletred", "midnightblue", "mistyrose", "navy", "olive", "olivedrab",
  "orange", "orangered", "orchid", "palegreen", "paleturquoise", "palevioletred",
  "peru", "pink", "plum", "powderblue", "purple", "rebeccapurple", "red",
  "rosybrown", "royalblue", "saddlebrown", "salmon", "sandybrown", "seagreen",
  "sienna", "silver", "skyblue", "slateblue", "slategray", "slategrey", "springgreen",
  "steelblue", "tan", "teal", "thistle", "turquoise", "violet", "wheat",
  "yellowgreen"];

var padding = 40;

var canvas = document.createElement('canvas');
var ctx = canvas.getContext("2d");
var metrics = ctx.measureText("0");
var charWidth = metrics.width * 2;
var offset = metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent + 6;

function draw(canvas, spi, pi) {
  var ctx = canvas.getContext("2d");

  var height = calcHeight(canvas, spi, pi);

  ctx.translate(10, height - 5);

  var x = 0;
  var y = 0;

  for (var i = 0; i < pi.length; i++) {
    ctx.fillText(pi[i], x, y);
    x = x + padding;
  }


  for (var j = 0; j < spi.length; j++) {
    var cycle = spi[j];
    ctx.strokeStyle = COLOR_POOL[j];

    for (var i = 0; i < cycle.length; i++) {
      var a = cycle[i];
      var b = cycle[(i + 1) % cycle.length];

      var aPos = pi.indexOf(a);
      var bPos = pi.indexOf(b);

      ctx.beginPath();
      ctx.lineWidth = 3;
      ctx.moveTo(aPos * padding, -offset);
      ctx.lineTo(aPos * padding + charWidth, -offset);
      ctx.stroke();

      var start = aPos * padding;
      var end = (bPos * padding) + charWidth;

      var width = (end - start) / 2;
      ctx.beginPath();
      ctx.lineWidth = 1;
      ctx.ellipse(start + width, -offset, Math.abs(width), Math.abs(width / 2), Math.PI, 0, Math.PI);
      ctx.stroke();
    }
  }
}

function calcHeight(canvas, spi, pi) {
  var ctx = canvas.getContext("2d");

  var maxHeight = 0;
  for (var j = 0; j < spi.length; j++) {
    var cycle = spi[j];

    for (var i = 0; i < cycle.length; i++) {
      var a = cycle[i];
      var b = cycle[(i + 1) % cycle.length];

      var aPos = pi.indexOf(a);
      var bPos = pi.indexOf(b);

      var start = aPos * padding;
      var end = (bPos * padding) + charWidth;

      if (Math.abs(end - start) / 4 > maxHeight) {
        maxHeight = Math.abs(end - start) / 4;
      }
    }
  }

  return maxHeight + offset + 10;
}
