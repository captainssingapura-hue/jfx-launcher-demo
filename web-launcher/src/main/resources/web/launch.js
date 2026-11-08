// Launch page: one row per environment. A click asks /token (same origin, so no CORS) for a
// signed fxsuite-<env>:// link, then hands it to Windows, which starts that environment's
// launcher. Values from the server are only ever set as text, never as HTML.

const ENVS = [
    { id: 'prod', label: 'Production', kind: 'prod', note: 'Dedicated install with its own signing key. Pinned version.' },
    { id: 'uat',  label: 'UAT',        kind: 'uat',  note: 'Dedicated install. Release candidate.' },
    { id: 'dev1', label: 'dev1',       kind: 'dev',  note: 'Shared dev install, --env=dev1. Latest version.' },
    { id: 'dev2', label: 'dev2',       kind: 'dev',  note: 'Same install, --env=dev2. Latest version.' },
];

const list = document.getElementById('envs');
const status = document.getElementById('status');
const buttons = [];

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

for (const env of ENVS) {
    const row = el('div', 'card env');
    row.append(el('span', 'pill ' + env.kind, env.id.toUpperCase()));

    const meta = el('span', 'meta');
    meta.append(el('b', '', env.label), ' · ', el('code', '', 'fxsuite-' + env.id + '://'),
                document.createElement('br'), el('small', '', env.note));
    row.append(meta);

    const button = el('button', '', 'Launch');
    button.addEventListener('click', () => launch(env.id));
    buttons.push(button);
    row.append(button);

    list.append(row);
}

function show(text, isError) {
    status.textContent = text;
    status.classList.toggle('error', !!isError);
}

async function launch(envId) {
    buttons.forEach(b => b.disabled = true);
    show('Requesting a launch token for ' + envId + '…');
    try {
        const response = await fetch('/token?app=hello&env=' + encodeURIComponent(envId));
        const body = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(body.error || 'token request failed (' + response.status + ')');

        show('Launching hello ' + body.ver + ' in ' + body.env
             + (body.dedicatedKey ? ', signed with its own key.' : ', signed with the shared dev key.'));
        window.location.href = body.url;   // hand fxsuite-<env>:// to Windows
    } catch (e) {
        show('Could not launch: ' + e.message, true);
    } finally {
        buttons.forEach(b => b.disabled = false);
    }
}
