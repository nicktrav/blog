<!DOCTYPE html>
<html lang="en">
    <head>
      {{ import "_styles.html.tpl" }}
    </head>
    <body class="hack">
      <div class="container">
        <header class="header">
          <p><b><a href="/">Home</a> >> Useful</b></p>
        </header>
        <div>
        <ul>
          {{ range $index, $page := .Pages }}
          <li><a href="/useful/{{ $page.Name }}">{{ $page.Name }}</a></li>
          {{ end }}
        </ul>
        </div>
      </div>
    </body>
</html>
