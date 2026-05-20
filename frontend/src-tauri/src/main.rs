#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::sync::Mutex;
use tauri::{Emitter, Manager, State};
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

/// State the webview reads via the `engine_base_url` command.
struct EngineState {
    base_url: Mutex<Option<String>>,
}

#[tauri::command]
fn engine_base_url(state: State<EngineState>) -> Option<String> {
    state.base_url.lock().ok().and_then(|g| g.clone())
}

fn main() {
    env_logger::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(EngineState { base_url: Mutex::new(None) })
        .invoke_handler(tauri::generate_handler![engine_base_url])
        .setup(|app| {
            let handle = app.handle().clone();

            // Spawn the bundled Java engine as a sidecar. The engine prints
            // "PORT=NNNNN" on the first stdout line; we parse it and stash
            // the base URL for the webview to read.
            let sidecar = app
                .shell()
                .sidecar("openrocketx-launcher")
                .expect("missing sidecar binary");
            let (mut rx, _child) = sidecar.spawn().expect("failed to launch engine");

            tauri::async_runtime::spawn(async move {
                while let Some(event) = rx.recv().await {
                    if let CommandEvent::Stdout(line) = event {
                        let line = String::from_utf8_lossy(&line).to_string();
                        if let Some(port_str) = line.strip_prefix("PORT=") {
                            let port: u16 = port_str.trim().parse().unwrap_or(0);
                            let url = format!("http://127.0.0.1:{}", port);
                            log::info!("engine base URL: {}", url);
                            let state: State<EngineState> = handle.state();
                            if let Ok(mut g) = state.base_url.lock() {
                                *g = Some(url.clone());
                            }
                            let _ = handle.emit("engine-ready", url);
                        } else {
                            log::debug!("engine: {}", line.trim_end());
                        }
                    } else if let CommandEvent::Stderr(line) = event {
                        log::warn!("engine stderr: {}", String::from_utf8_lossy(&line));
                    }
                }
            });

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
