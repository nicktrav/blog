<!DOCTYPE html>
<html lang="en">
<head>
    <title>{{ .Title }}</title>
    {{ import "_styles.html.tpl" }}
    <base target="_blank">
</head>
<header>
    <a href="/" target="_self">Nick Travers</a>
    <aside></aside>
</header>
<body>
{{ .HTML }}
</body>
<footer>
    <a href="/" target="_self">Nick Travers</a>
    <aside></aside>
</footer>
</html>
