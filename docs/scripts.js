// Osyster website. Fades sections in, and fills GitHub numbers, the latest version, and the
// direct APK link from the GitHub API. Without it the page still works: buttons open the
// latest release page and the numbers stay as dashes.

(() => {
    const reveal = document.querySelectorAll('.reveal');
    if (!('IntersectionObserver' in window)) {
        reveal.forEach(element => element.classList.add('seen'));
        return;
    }
    const watcher = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('seen');
                watcher.unobserve(entry.target);
            }
        });
    }, { rootMargin: '0px 0px -10% 0px' });
    reveal.forEach(element => watcher.observe(element));
})();

(async () => {
    const api = 'https://api.github.com/repos/qtremors/osyster';
    const format = value => new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(value);
    const set = (id, value) => {
        const element = document.getElementById(id);
        if (element) element.textContent = value;
    };

    try {
        const [repo, latest] = await Promise.all([
            fetch(api).then(response => response.ok ? response.json() : null),
            fetch(`${api}/releases/latest`).then(response => response.ok ? response.json() : null)
        ]);

        if (repo) {
            set('gh-stars', format(repo.stargazers_count));
            set('gh-forks', format(repo.forks_count));
        }

        if (latest) {
            // Dynamic version and a direct link to the APK in the latest release.
            const apk = (latest.assets || []).find(asset => asset.name.endsWith('.apk'));
            document.querySelectorAll('.release').forEach(label => { label.textContent = `Download ${latest.tag_name}`; });
            if (apk) document.querySelectorAll('.apk-link').forEach(link => { link.href = apk.browser_download_url; });
            document.querySelectorAll('.version-note').forEach(note => { note.textContent = `Latest: ${latest.tag_name}. `; });
            set('gh-latest', format((latest.assets || []).reduce((sum, asset) => sum + asset.download_count, 0)));
        }

        // Total downloads across every release page.
        let total = 0;
        let page = `${api}/releases?per_page=100`;
        while (page) {
            const response = await fetch(page);
            if (!response.ok) break;
            (await response.json()).forEach(release => release.assets.forEach(asset => { total += asset.download_count; }));
            const next = /<([^>]+)>;\s*rel="next"/.exec(response.headers.get('Link') || '');
            page = next ? next[1] : null;
        }
        if (total > 0) set('gh-total', format(total));
    } catch {
        // Offline or rate limited: links still point at the releases page.
    }
})();
