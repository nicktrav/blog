<!DOCTYPE html>
<html lang="en">
    <head>
      {{ import "_styles.html.tpl" }}
    </head>
    <body class="hack">
      <div class="container">
        <header class="header">
          <p><b><a href="/">Home</a> >> Posts</b></p>
        </header>
        <div>
        <ul>
          {{ range $index, $page := .Pages }}
          <li><a href="/posts/{{ $page.Name }}">{{ $page.Name }}</a></li>
          {{ end }}
        </ul>
        </div>
      </div>
    </body>
</html>
