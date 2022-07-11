<!DOCTYPE html>
<html lang="en">
<head>
    <title>{{ .Title }}</title>
    {{ import "_styles.html.tpl" }}
    <base target="_blank">
</head>
<body>
<div id="topbar"></div>
<main>
<header>
    <a href="/" target="_self">Nick Travers</a>
    <aside></aside>
</header>
{{ .HTML }}
</body>
<footer>
    <a href="/" target="_self">Nick Travers</a>
    <aside></aside>
</footer>
</main>
</html>
