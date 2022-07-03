<!DOCTYPE html>
<html lang="en">
<head>
    <title>nicktrave.rs</title>
    {{ import "_styles.html.tpl" }}
</head>
<header>
    <a href="/" target="_self">Nick Travers</a>
    <aside></aside>
</header>
<body>
<h2>Posts</h2>
<ul>
  {{ range $page := .Posts }}
  <li><p><a href="{{ $page.URL }}">{{ $page.Name }}</a> - <i>{{ $page.Date }}</i></p></li>
  {{ end }}
</ul>
<h2>Useful</h2>
<ul>
  {{ range $page := .Useful }}
  <li><p><a href="{{ $page.URL }}">{{ $page.Name }}</a></p></li>
  {{ end }}
</ul>
</body>
</html>
