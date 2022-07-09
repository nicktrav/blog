<!DOCTYPE html>
<html lang="en">
<head>
    <title>nicktrave.rs</title>
    {{ import "_styles.html.tpl" }}
</head>
<body>
<div id="topnav"></div>
<main>
<header>
  <a href="/" target="_self">Nick Travers</a>
  <aside></aside>
</header>
<h1>Posts</h1>
<ul>
  {{ range $page := .Posts }}
  <li><p><a href="{{ $page.URL }}">{{ $page.Name }}</a> - <i>{{ $page.Date }}</i></p></li>
  {{ end }}
</ul>
<h1>Useful</h1>
<p>
    Stuff in here is just a random collection of things that I've picked up
    along the way, inspired by Reddit's <a
        href="https://www.reddit.com/r/todayilearned">r/til</a>.
</p>
<p>
    Think Bash one-liners and useful CLI commands for various things, structured
    in a way that makes it easy for me to search for when I need it.
</p>
<ul>
  {{ range $page := .Useful }}
  <li><p><a href="{{ $page.URL }}">{{ $page.Name }}</a></p></li>
  {{ end }}
</ul>
</main>
</body>
</html>
